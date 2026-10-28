package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación de {@link AuthorizationService}.
 *
 * <p>El bean se registra con el nombre explícito {@code "authorizationService"} porque las
 * expresiones {@code @PreAuthorize} lo referencian como {@code @authorizationService}; sin ese
 * nombre, Spring lo llamaría "authorizationServiceImpl" y las expresiones no lo encontrarían.</p>
 */
@Service("authorizationService")
@Transactional(readOnly = true)
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public AuthorizationServiceImpl(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    // SecurityContextHolder guarda la autenticación de la petición en curso (la coloca JwtAuthenticationFilter).
    @Override
    public Optional<SecurityPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return Optional.of(principal);
        }
        // Un usuario anónimo tiene como principal el texto "anonymousUser", que no es un SecurityPrincipal.
        return Optional.empty();
    }

    @Override
    public boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }

    @Override
    public boolean isStaffOrAdmin() {
        return hasAuthority("ROLE_STAFF") || hasAuthority("ROLE_ADMIN");
    }

    // Busca al usuario por email y verifica que sea el mismo que el autenticado.
    @Override
    public boolean isSelfByEmail(String email) {
        if (email == null) {
            return false;
        }
        return isSameUser(userRepository.findByEmailIgnoreCase(email));
    }

    // Igual que isSelfByEmail pero buscando por username.
    @Override
    public boolean isSelfByUsername(String username) {
        if (username == null) {
            return false;
        }
        return isSameUser(userRepository.findByUsername(username));
    }

    // Una sola consulta: ¿existe un ticket con ese código que pertenezca al id del usuario autenticado?
    // Si el ticket no existe, el resultado es false (igual que si fuera de otro usuario).
    @Override
    public boolean isTicketOwner(String ticketCode) {
        if (ticketCode == null) {
            return false;
        }
        return currentPrincipal()
                .map(p -> ticketRepository.existsByTicketCodeAndUserId(ticketCode, p.getId()))
                .orElse(false);
    }

    // ---- helpers ----

    // Compara los ids (clave primaria) y no el email/username, que son datos que el usuario podría cambiar.
    private boolean isSameUser(Optional<User> requested) {
        // Si el recurso no existe devolvemos false (⇒ 403) para no revelar existencia (D6)
        return currentPrincipal()
                .flatMap(p -> requested.map(u -> u.getId().equals(p.getId())))
                .orElse(false);
    }

    // ¿El usuario actual tiene esa autoridad? (p. ej. "ROLE_ADMIN"). Sin usuario autenticado devuelve false.
    private boolean hasAuthority(String authority) {
        return currentPrincipal()
                .map(p -> p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority)))
                .orElse(false);
    }
}