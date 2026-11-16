package com.pulsepass.service;

import com.pulsepass.security.SecurityPrincipal;

import java.util.Optional;

/**
 * Reglas de autorización que no se pueden expresar solo con roles, por ejemplo "¿este recurso es
 * del usuario que hace la petición?". Se usa de dos maneras:
 * <ol>
 *   <li>Desde expresiones {@code @PreAuthorize}, p. ej.
 *       {@code @authorizationService.isSelfByEmail(#email)} (ver TicketService y UserService).</li>
 *   <li>Directamente desde servicios, p. ej. {@code EventServiceImpl} llama a {@code isStaffOrAdmin()}
 *       para decidir qué eventos puede ver quien consulta.</li>
 * </ol>
 * "Usuario actual" = el que autenticó {@code JwtAuthenticationFilter} en esta petición.
 */
public interface AuthorizationService {

    /** Usuario autenticado de la petición actual; vacío si es anónimo o no hay autenticación. */
    Optional<SecurityPrincipal> currentPrincipal();

    /** ¿El usuario actual tiene rol ADMIN? */
    boolean isAdmin();

    /** ¿El usuario actual tiene rol STAFF o ADMIN? */
    boolean isStaffOrAdmin();

    /** ¿El email pertenece al usuario autenticado? (compara por ID persistente) */
    boolean isSelfByEmail(String email);

    /** ¿El username pertenece al usuario autenticado? (compara por ID persistente) */
    boolean isSelfByUsername(String username);

    /** ¿El ticket pertenece al usuario autenticado? (ticket inexistente ⇒ false) */
    boolean isTicketOwner(String ticketCode);
}