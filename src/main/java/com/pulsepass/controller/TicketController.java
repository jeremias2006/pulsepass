package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Prefijo "/api" (y no "/api/tickets") porque el endpoint de tickets pagados
// cuelga de /api/events/{eventCode}/... pero pertenece a TicketService.
/**
 * Endpoints REST de tickets: comprar, consultar, cancelar y marcar como usado.
 * Quién puede hacer cada cosa se indica en cada método (reglas por URL en {@code SecurityConfig} y
 * {@code @PreAuthorize} en {@code TicketService}).
 *
 * <p>Capa Controller: solo traduce entre HTTP y Java. Lee la petición (ruta, parámetros, cuerpo JSON),
 * delega TODO el trabajo en el servicio y devuelve un {@code ResponseEntity} con el código HTTP y el
 * cuerpo. No contiene reglas de negocio ni habla con los repositorios.</p>
 *
 * <p>Anotaciones habituales: {@code @RestController} (cada método devuelve datos que Spring serializa
 * a JSON), {@code @RequestMapping} (prefijo común de las rutas) y {@code @GetMapping} /
 * {@code @PostMapping} / {@code @PatchMapping} (verbo HTTP + ruta). Los errores no se manejan aquí:
 * las excepciones las traduce {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;   // inyectado por constructor

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // POST /api/tickets  ->  201 Created
    // Solo USER, y el userEmail del body debe ser el del usuario autenticado (FR-SECTKT-01).
    // El body NO incluye precio: lo calcula el servidor según el tipo de ticket.
    @PostMapping("/tickets")
    public ResponseEntity<TicketResponse> purchase(
            @Valid @RequestBody PurchaseTicketRequest request) {

        TicketResponse response = ticketService.purchase(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET /api/tickets/by-user?email=andrea@mail.com
    // El propietario o ADMIN (FR-SECTKT-03). Spring da prioridad a esta ruta literal sobre /{ticketCode}.
    @GetMapping("/tickets/by-user")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(
            @RequestParam String email) {

        return ResponseEntity.ok(ticketService.findByUserEmail(email));
    }

    // GET /api/tickets/{ticketCode}
    // El propietario del ticket, o STAFF/ADMIN (FR-SECTKT-02).
    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(
            @PathVariable String ticketCode) {

        return ResponseEntity.ok(ticketService.findByCode(ticketCode));
    }

    // PATCH /api/tickets/{ticketCode}/cancel
    // ADMIN, o USER propietario (FR-SECTKT-05). Solo cambia el estado del ticket (PAID -> CANCELLED).
    @PatchMapping("/tickets/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(
            @PathVariable String ticketCode) {

        return ResponseEntity.ok(ticketService.cancel(ticketCode));
    }

    // PATCH /api/tickets/{ticketCode}/use
    // Solo STAFF/ADMIN (FR-SECTKT-06). Marca el ticket como usado (PAID -> USED).
    @PatchMapping("/tickets/{ticketCode}/use")
    public ResponseEntity<TicketResponse> markAsUsed(
            @PathVariable String ticketCode) {

        return ResponseEntity.ok(ticketService.markAsUsed(ticketCode));
    }

    // GET /api/events/{eventCode}/tickets/paid
    // Solo STAFF/ADMIN (FR-SECTKT-04). Cuelga de /events pero lo atiende TicketService (ver nota de la clase).
    @GetMapping("/events/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTicketsByEvent(
            @PathVariable String eventCode) {

        return ResponseEntity.ok(ticketService.findPaidTicketsByEvent(eventCode));
    }
}
