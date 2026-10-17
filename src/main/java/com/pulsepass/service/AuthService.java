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

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Clock clock;

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

        User user = userRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("User not found after registration"));

        credentialRepository.save(new UserCredential(
                user,
                passwordEncoder.encode(request.password()),
                EnumSet.of(Role.USER),
                LocalDateTime.now(clock)));

        log.info("register_success userId={}", created.id());
        return created;   // contrato anterior: sin hash ni password
    }

    /** FR-SECAUTH-002/003/004. Siempre el MISMO error ante cualquier fallo (no revela si el usuario existe). */
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.identifier().trim(), request.password()));

            SecurityPrincipal principal = (SecurityPrincipal) authentication.getPrincipal();
            String token = jwtService.generateAccessToken(principal.getId());

            log.info("login_success userId={}", principal.getId());
            return new AuthResponse(token, "Bearer", jwtService.getExpiresInSeconds());

        } catch (AuthenticationException ex) {
            // Incluye: usuario inexistente, contraseña mala, cuenta inactiva (DisabledException)
            log.warn("login_failed reason={}", ex.getClass().getSimpleName());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    /** FR-SECAUTH-010 */
    public AuthenticatedUserResponse me(SecurityPrincipal principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority().substring("ROLE_".length()))
                .sorted()
                .toList();
        return new AuthenticatedUserResponse(
                principal.getId(), principal.getUsername(), principal.getEmail(), roles);
    }
}
