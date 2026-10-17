package com.pulsepass.service;

import com.pulsepass.security.SecurityPrincipal;

import java.util.Optional;

public interface AuthorizationService {

    Optional<SecurityPrincipal> currentPrincipal();

    boolean isAdmin();

    boolean isStaffOrAdmin();

    /** ¿El email pertenece al usuario autenticado? (compara por ID persistente) */
    boolean isSelfByEmail(String email);

    /** ¿El username pertenece al usuario autenticado? (compara por ID persistente) */
    boolean isSelfByUsername(String username);

    /** ¿El ticket pertenece al usuario autenticado? (ticket inexistente ⇒ false) */
    boolean isTicketOwner(String ticketCode);
}