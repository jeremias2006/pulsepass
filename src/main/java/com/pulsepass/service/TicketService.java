package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Contrato de la lógica de negocio de tickets: comprar, consultar, cancelar y marcar como usado.
 * La implementación es {@code TicketServiceImpl}.
 *
 * <p><b>Seguridad (segunda capa).</b> Cada método declara su regla de acceso con
 * {@code @PreAuthorize}. Spring la evalúa ANTES de ejecutar el método porque
 * {@code SecurityConfig} activa {@code @EnableMethodSecurity}. La primera capa son las reglas por
 * URL de {@code SecurityConfig}; esta segunda capa vive en el servicio, así que se aplica sin
 * importar quién lo invoque.</p>
 *
 * <p><b>Cómo leer las expresiones:</b> {@code hasRole('X')} revisa el rol del usuario
 * autenticado; {@code @authorizationService} es el bean {@code AuthorizationServiceImpl};
 * {@code #request} y {@code #ticketCode} son los parámetros del método.</p>
 */
public interface TicketService {

    /**
     * Compra un ticket. Reglas de negocio: BR-TICKET-001 a 009 (ver la implementación).
     *
     * <p>FR-SECTKT-01: solo USER, y el userEmail del body debe ser el del principal
     * (nadie puede comprar "a nombre de" otro usuario).</p>
     */
    @PreAuthorize("hasRole('USER') and @authorizationService.isSelfByEmail(#request.userEmail())")
    TicketResponse purchase(PurchaseTicketRequest request);

    /**
     * Busca un ticket por su código.
     *
     * <p>FR-SECTKT-02: propietario, o STAFF/ADMIN. Un USER que no es dueño (o que pide un código
     * inexistente) recibe 403 en ambos casos: así no se revela qué códigos existen.</p>
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN') or @authorizationService.isTicketOwner(#ticketCode)")
    TicketResponse findByCode(String ticketCode);

    /**
     * Lista los tickets de un usuario, del más reciente al más antiguo (por fecha de compra).
     *
     * <p>FR-SECTKT-03: propietario, o ADMIN.</p>
     */
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isSelfByEmail(#email)")
    List<TicketResponse> findByUserEmail(String email);

    /**
     * Lista los tickets en estado PAID de un evento.
     *
     * <p>FR-SECTKT-04: solo STAFF/ADMIN.</p>
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    /**
     * Cancela un ticket (PAID -> CANCELLED). Reglas de negocio: BR-TICKET-010 a 012.
     *
     * <p>FR-SECTKT-05: ADMIN, o USER propietario. La autorización se evalúa ANTES de mutar estado.</p>
     */
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @authorizationService.isTicketOwner(#ticketCode))")
    TicketResponse cancel(String ticketCode);

    /**
     * Marca un ticket como usado (PAID -> USED). Reglas de negocio: BR-TICKET-013 y 014.
     *
     * <p>FR-SECTKT-06: solo STAFF/ADMIN.</p>
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    TicketResponse markAsUsed(String ticketCode);
}
