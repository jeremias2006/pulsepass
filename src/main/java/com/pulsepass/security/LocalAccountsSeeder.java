package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

/**
 * Crea las cuentas ADMIN y STAFF de desarrollo al arrancar la aplicación.
 *
 * <p>¿Por qué existe? El registro público ({@code POST /api/users}) solo crea usuarios con rol USER,
 * por lo que no hay otra forma de tener un ADMIN o un STAFF para probar los endpoints protegidos.</p>
 *
 * <ul>
 *   <li>{@code @Profile("local")}: solo se ejecuta si el perfil "local" está activo
 *       (por ejemplo, {@code SPRING_PROFILES_ACTIVE=local}); en cualquier otro entorno no existe.</li>
 *   <li>{@code ApplicationRunner}: Spring ejecuta {@code run} una vez, justo cuando la aplicación termina de arrancar.</li>
 *   <li>Las contraseñas no están en el código: vienen de las variables de entorno
 *       PULSEPASS_BOOTSTRAP_ADMIN_PASSWORD y PULSEPASS_BOOTSTRAP_STAFF_PASSWORD.</li>
 * </ul>
 */
@Component
@Profile("local")
public class LocalAccountsSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalAccountsSeeder.class);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String adminPassword;
    private final String staffPassword;

    public LocalAccountsSeeder(UserRepository userRepository,
                               UserCredentialRepository credentialRepository,
                               PasswordEncoder passwordEncoder,
                               Clock clock,
                               @Value("${pulsepass.bootstrap.admin-password:}") String adminPassword,
                               @Value("${pulsepass.bootstrap.staff-password:}") String staffPassword) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminPassword = adminPassword;
        this.staffPassword = staffPassword;
    }

    // @Transactional: el User, su perfil y su credencial de cada cuenta se guardan juntos o no se guarda nada.
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("admin", "admin@pulsepass.local", "Admin", "PulsePass", Role.ADMIN, adminPassword);
        seed("laura", "laura@pulsepass.local", "Laura", "Staff", Role.STAFF, staffPassword);
    }

    /** Crea una cuenta con el rol indicado, salvo que la contraseña sea inválida o la cuenta ya exista. */
    private void seed(String username, String email, String firstName, String lastName,
                      Role role, String rawPassword) {
        // Misma regla que el registro (RegisterUserRequest): entre 12 caracteres y 72 bytes (límite de BCrypt).
        // Si no se configuró una contraseña válida, esa cuenta se omite con una advertencia en el log.
        if (rawPassword == null || rawPassword.length() < 12
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            log.warn("seed_skipped username={} reason=password_missing_or_invalid", username);
            return;
        }
        if (userRepository.existsByUsername(username)) {
            return;   // idempotente
            // (idempotente = ejecutar la aplicación varias veces no duplica ni modifica cuentas ya creadas)
        }
        // Datos de perfil de relleno (fecha de nacimiento fija) solo para cumplir el modelo de datos.
        User user = new User(username, email);
        user.assignProfile(new UserProfile(firstName, lastName, null, null, LocalDate.of(1990, 1, 1)));
        userRepository.save(user);

        // La contraseña se guarda como hash BCrypt, igual que en el registro normal, con el rol recibido.
        credentialRepository.save(new UserCredential(
                user, passwordEncoder.encode(rawPassword), EnumSet.of(role), LocalDateTime.now(clock)));
        log.info("seed_created username={} role={}", username, role);
    }
}
