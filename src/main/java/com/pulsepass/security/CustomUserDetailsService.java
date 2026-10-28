package com.pulsepass.security;

import com.pulsepass.domain.UserCredential;
import com.pulsepass.repository.UserCredentialRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Carga usuarios para Spring Security desde nuestra base de datos. Se usa en dos momentos:
 * <ol>
 *   <li><b>Login:</b> el {@code AuthenticationManager} llama a {@link #loadUserByUsername(String)} para
 *       obtener el hash de la contraseña y compararlo con la que escribió el usuario.</li>
 *   <li><b>Cada petición con token:</b> {@code JwtAuthenticationFilter} llama a
 *       {@link #loadUserById(Long)}. El token solo trae el id; los roles y el estado de la cuenta se
 *       leen de la base en cada petición, así que quitar un rol o desactivar a un usuario surte efecto
 *       de inmediato, sin esperar a que el token venza.</li>
 * </ol>
 * Es {@code @Transactional(readOnly = true)} porque {@code SecurityPrincipal.from} necesita recorrer
 * relaciones LAZY (usuario y roles) y eso requiere una transacción abierta.
 * Los mensajes de error son genéricos para no revelar si un usuario existe.
 */
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserCredentialRepository credentialRepository;

    public CustomUserDetailsService(UserCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    /** Login: el identificador puede ser email (contiene "@") o username. */
    @Override
    public SecurityPrincipal loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Optional<UserCredential> found = identifier.contains("@")
                ? credentialRepository.findByUserEmailIgnoreCase(identifier)
                : credentialRepository.findByUserUsername(identifier);

        return found.map(SecurityPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Credentials not found"));
    }

    /** Filtro JWT: revalida usuario, estado y roles actuales en CADA request. */
    public SecurityPrincipal loadUserById(Long userId) throws UsernameNotFoundException {
        return credentialRepository.findByUserId(userId)
                .map(SecurityPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Credentials not found"));
    }
}