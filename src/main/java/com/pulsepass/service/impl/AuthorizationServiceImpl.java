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

@Service("authorizationService")
@Transactional(readOnly = true)
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public AuthorizationServiceImpl(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Optional<SecurityPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return Optional.of(principal);
        }
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

    @Override
    public boolean isSelfByEmail(String email) {
        if (email == null) {
            return false;
        }
        return isSameUser(userRepository.findByEmailIgnoreCase(email));
    }

    @Override
    public boolean isSelfByUsername(String username) {
        if (username == null) {
            return false;
        }
        return isSameUser(userRepository.findByUsername(username));
    }

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

    private boolean isSameUser(Optional<User> requested) {
        // Si el recurso no existe devolvemos false (⇒ 403) para no revelar existencia (D6)
        return currentPrincipal()
                .flatMap(p -> requested.map(u -> u.getId().equals(p.getId())))
                .orElse(false);
    }

    private boolean hasAuthority(String authority) {
        return currentPrincipal()
                .map(p -> p.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority)))
                .orElse(false);
    }
}