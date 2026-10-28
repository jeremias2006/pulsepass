package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Casos de uso de autenticación: registrarse, iniciar sesión y consultar "quién soy".
 *
 * <p>Es un orquestador: no guarda contraseñas ni firma tokens por sí mismo, sino que coordina a
 * {@link UserService} (datos del usuario), el repositorio de credenciales (hash BCrypt y roles),
 * el {@code AuthenticationManager} de Spring Security (valida identificador y contraseña) y
 * {@link JwtService} (emite el token de acceso).</p>
 */
@Service
public class AuthService {

    // Logger SLF4J. Solo se registran ids y motivos de error; nunca contraseñas ni tokens.
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Dependencias inmutables (final), inyectadas por Spring a través del constructor.
    private final UserService userService;                       // crea User + UserProfile (reglas de negocio)
    private final UserRepository userRepository;                 // recargar el User recién creado
    private final UserCredentialRepository credentialRepository; // guarda hash de contraseña y roles
    private final PasswordEncoder passwordEncoder;               // BCrypt (definido en SecurityConfig)
    private final AuthenticationManager authenticationManager;   // valida identificador + contraseña
    private final JwtService jwtService;                         // genera el token de acceso
    private final Clock clock;                                   // reloj inyectable (permite fijar la hora en pruebas)

    public AuthService(UserService userService,
                       UserRepository userRepository,
                       UserCredentialRepository credentialRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       Clock clock) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    /**
     * FR-SECAUTH-001. User + UserProfile + credencial BCrypt + ROLE_USER en UNA transacción.
     * UserServiceImpl.register (@Transactional) se une a esta transacción: si falla el hash
     * o el insert de la credencial, se revierte también el usuario.
     * El rol siempre es USER: el request no puede pedir otro (NFR-SEC009).
     */
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        UserResponse created = userService.register(request);   // aplica unicidad, edad, etc.

        // Se recarga la entidad User recién creada para enlazarla con su credencial (el DTO solo trae el id).
        User user = userRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("User not found after registration"));

        // La contraseña se guarda SOLO como hash BCrypt (passwordEncoder.encode), nunca en texto plano.
        // El rol es siempre USER. ADMIN y STAFF no se pueden obtener registrándose.
        credentialRepository.save(new UserCredential(
                user,
                passwordEncoder.encode(request.password()),
                EnumSet.of(Role.USER),
                LocalDateTime.now(clock)));

        log.info("register_success userId={}", created.id());
        return created;   // contrato anterior: sin hash ni password
    }

    /**
     * FR-SECAUTH-002/003/004. Siempre el MISMO error ante cualquier fallo (no revela si el usuario existe).
     *
     * <p>El identificador puede ser email o username. Si las credenciales son correctas devuelve un
     * token JWT de tipo "Bearer"; el cliente debe enviarlo luego en la cabecera
     * {@code Authorization: Bearer <token>}.</p>
     *
     * @throws BadCredentialsException ante cualquier fallo de autenticación (se traduce a 401)
     */
    public AuthResponse login(LoginRequest request) {
        try {
            // Spring Security carga la credencial, compara la contraseña con el hash BCrypt y verifica que la
            // cuenta esté activa. "unauthenticated(...)" crea un token de entrada que todavía NO está autenticado.
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.identifier().trim(), request.password()));

            // Si la autenticación fue exitosa, el principal es un SecurityPrincipal (lo arma CustomUserDetailsService).
            SecurityPrincipal principal = (SecurityPrincipal) authentication.getPrincipal();
            // Emite el JWT de acceso para ese usuario (JwtService lo firma y le pone vencimiento).
            String token = jwtService.generateAccessToken(principal.getId());

            log.info("login_success userId={}", principal.getId());
            // Respuesta: el token, su tipo ("Bearer") y cuántos segundos de vigencia le quedan.
            return new AuthResponse(token, "Bearer", jwtService.getExpiresInSeconds());

        } catch (AuthenticationException ex) {
            // Incluye: usuario inexistente, contraseña mala, cuenta inactiva (DisabledException)
            // El motivo real solo queda en el log del servidor; al cliente siempre se le responde el mismo
            // mensaje genérico, para que nadie pueda averiguar qué usuarios existen (enumeración de usuarios).
            log.warn("login_failed reason={}", ex.getClass().getSimpleName());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    /**
     * FR-SECAUTH-010. Devuelve los datos del usuario autenticado (id, username, email y roles).
     * Spring guarda los roles como "ROLE_USER", "ROLE_ADMIN"...; aquí se les quita el prefijo "ROLE_"
     * y se ordenan alfabéticamente para que la respuesta sea estable.
     */
    public AuthenticatedUserResponse me(SecurityPrincipal principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority().substring("ROLE_".length()))
                .sorted()
                .toList();
        return new AuthenticatedUserResponse(
                principal.getId(), principal.getUsername(), principal.getEmail(), roles);
    }
}
