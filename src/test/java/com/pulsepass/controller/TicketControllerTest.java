package com.pulsepass.controller;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.security.SecurityErrorWriter;
import com.pulsepass.service.AuthorizationService;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la CAPA WEB (HTTP) del controlador, sin base de datos ni lógica de negocio real. Se envían peticiones HTTP simuladas
 * con {@code MockMvc} y se comprueba la ruta, el código de respuesta, el JSON devuelto, las validaciones ({@code @Valid}) y cómo
 * {@code GlobalExceptionHandler} traduce las excepciones.
 *
 * <ul>
 *   <li>{@code @WebMvcTest}: levanta solo Spring MVC y seguridad, y únicamente el controlador indicado.</li>
 *   <li>{@code @Import}: agrega lo que @WebMvcTest no carga solo: GlobalExceptionHandler, SecurityConfig y los manejadores de
 *       errores 401/403.</li>
 *   <li>{@code @MockitoBean}: reemplaza el servicio por un doble. {@code when(...).thenReturn(...)} o {@code thenThrow(...)} define
 *       lo que "responde" el servicio, así solo se prueba el controlador. JwtService, CustomUserDetailsService y
 *       AuthorizationService también se simulan: son dependencias de la seguridad y así el contexto arranca sin el resto de la aplicación.</li>
 *   <li>{@code jsonPath("$.campo")}: lee un campo del JSON de respuesta. {@code verify(servicio, never())} demuestra que el
 *       controlador rechazó la petición ANTES de llegar al servicio.</li>
 * </ul>
 *
 * {@code @WithMockUser(roles = "USER")} a nivel de clase hace que todas las pruebas se ejecuten como un USER autenticado;
 * las que requieren STAFF lo sobrescriben en el propio método. Las reglas de negocio (edad, cupo...) son del servicio, aquí simulado:
 * estas pruebas solo verifican que sus excepciones se traducen al código HTTP correcto.
 */
@WebMvcTest(TicketController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SecurityErrorWriter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@WithMockUser(roles = "USER")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    // SecurityConfig los necesita para construir la cadena de filtros
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private AuthorizationService authorizationService;

    // Cuerpo JSON de compra válido: NO incluye precio (lo calcula el servidor).
    private static final String VALID_BODY = """
            {
              "userEmail": "andrea@mail.com",
              "eventCode": "CMF-2026",
              "type": "GENERAL"
            }
            """;

    // Ticket de ejemplo con el estado indicado, que devuelve el servicio simulado.
    private TicketResponse ticket(TicketStatus ticketStatus) {
        return new TicketResponse(
                1L,
                "TKT-A1B2C3D4",
                TicketType.GENERAL,
                new BigDecimal("50.00"),
                ticketStatus,
                LocalDateTime.of(2026, 10, 4, 20, 0),
                "andrea@mail.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );
    }

    // TEST-CTRL-TKT-001 / AC-CTRL009: compra válida -> 201 + TicketResponse
    // Compra válida: 201 Created con el ticket (código, tipo, estado PAID, email y evento).
    @Test
    void shouldPurchaseTicket() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-A1B2C3D4"))
                .andExpect(jsonPath("$.type").value("GENERAL"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.userEmail").value("andrea@mail.com"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    // TEST-CTRL-TKT-002: request inválido -> 400 + details, Service nunca invocado
    // Email inválido, código vacío y tipo ausente: 400 con un mensaje por campo en details. El servicio nunca se invoca.
    @Test
    void shouldReturn400WhenPurchaseRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail": "no-es-un-email",
                                  "eventCode": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").value("Email format is invalid"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    // Extra: tipo de ticket fuera del enum -> 400
    // "PLATINUM" no existe en TicketType: el JSON no se puede convertir y responde 400 "Malformed or invalid JSON request".
    @Test
    void shouldReturn400WhenTicketTypeIsNotInEnum() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("GENERAL", "PLATINUM")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(ticketService, never()).purchase(any());
    }

    // TEST-CTRL-TKT-003: usuario inexistente -> 404
    // BR-TICKET-001: el servicio lanza ResourceNotFoundException -> 404.
    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@mail.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found: andrea@mail.com"));
    }

    // TEST-CTRL-TKT-004 / AC-CTRL010: menor de edad (regla de Service) -> 409
    // BR-TICKET-006: edad insuficiente. Es regla de negocio -> BusinessRuleException -> 409.
    @Test
    void shouldReturn409WhenUserDoesNotMeetMinimumAge() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "User does not meet minimum age of 18 for event CMF-2026"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("User does not meet minimum age of 18 for event CMF-2026"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // Extra: usuario inactivo / evento no publicado / sin capacidad -> 409
    // BR-TICKET-007: cupo agotado -> 409 con el mensaje de la regla.
    @Test
    void shouldReturn409WhenEventHasNoCapacityLeft() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("Event has no capacity left: CMF-2026"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Event has no capacity left: CMF-2026"));
    }

    // TEST-CTRL-TKT-005: consultar ticket -> 200
    // Consulta por código: 200 con el ticket y el nombre del evento.
    @Test
    void shouldReturnTicketByCode() throws Exception {
        when(ticketService.findByCode("TKT-A1B2C3D4")).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TKT-A1B2C3D4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TKT-A1B2C3D4"))
                .andExpect(jsonPath("$.eventName").value("Caribbean Music Fest 2026"));

        verify(ticketService).findByCode("TKT-A1B2C3D4");
    }

    // FR-CTRLTKT-002: ticket inexistente -> 404
    // Ticket inexistente: 404.
    @Test
    void shouldReturn404WhenTicketDoesNotExist() throws Exception {
        when(ticketService.findByCode("TKT-NOPE"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: TKT-NOPE"));

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TKT-NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found: TKT-NOPE"));
    }

    // TEST-CTRL-TKT-006: tickets por usuario -> 200
    // Tickets de un usuario: 200 con una lista. Verifica que "/by-user" no se tomó como {ticketCode}.
    @Test
    void shouldReturnTicketsByUserEmail() throws Exception {
        when(ticketService.findByUserEmail("andrea@mail.com"))
                .thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@mail.com"));

        verify(ticketService).findByUserEmail("andrea@mail.com");
        // "by-user" no debe interpretarse como {ticketCode}
        verify(ticketService, never()).findByCode(any());
    }

    // Extra: falta ?email= -> 400 (no 500)
    // Falta ?email=: 400 (no 500) y el servicio nunca se invoca.
    @Test
    void shouldReturn400WhenEmailParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/tickets/by-user"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());

        verify(ticketService, never()).findByUserEmail(any());
    }

    // TEST-CTRL-TKT-007: tickets PAID por evento -> 200
    // Se ejecuta como STAFF (rol que exige esta ruta): 200 con los tickets PAID del evento.
    @Test
    @WithMockUser(roles = "STAFF")
    void shouldReturnPaidTicketsByEvent() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026"))
                .thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    // TEST-CTRL-TKT-008 / AC-CTRL011: cancelar ticket PAID -> 200
    // Cancelar (PATCH): 200 y el ticket sale en estado CANCELLED.
    @Test
    void shouldCancelTicket() throws Exception {
        when(ticketService.cancel("TKT-A1B2C3D4")).thenReturn(ticket(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TKT-A1B2C3D4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TKT-A1B2C3D4");
    }

    // TEST-CTRL-TKT-009 / AC-CTRL012: cancelar ticket USED -> 409
    // BR-TICKET-010/011: cancelar un ticket que no es PAID -> 409 con el mensaje de la regla.
    @Test
    void shouldReturn409WhenTicketCannotBeCancelled() throws Exception {
        when(ticketService.cancel("TKT-A1B2C3D4"))
                .thenThrow(new BusinessRuleException(
                        "Only PAID tickets can be cancelled. Current status: USED"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TKT-A1B2C3D4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Only PAID tickets can be cancelled. Current status: USED"));
    }

    // TEST-CTRL-TKT-010 / AC-CTRL013: usar ticket PAID -> 200
    // Se ejecuta como STAFF: marcar como usado devuelve 200 y el ticket en estado USED.
    @Test
    @WithMockUser(roles = "STAFF")
    void shouldMarkTicketAsUsed() throws Exception {
        when(ticketService.markAsUsed("TKT-A1B2C3D4")).thenReturn(ticket(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TKT-A1B2C3D4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TKT-A1B2C3D4");
    }

    // TEST-CTRL-TKT-011 / AC-CTRL014: usar ticket CANCELLED -> 409
    // BR-TICKET-014: usar un ticket CANCELLED -> 409.
    @Test
    @WithMockUser(roles = "STAFF")
    void shouldReturn409WhenTicketCannotBeUsed() throws Exception {
        when(ticketService.markAsUsed("TKT-A1B2C3D4"))
                .thenThrow(new BusinessRuleException(
                        "Cancelled ticket cannot be used: TKT-A1B2C3D4"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TKT-A1B2C3D4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Cancelled ticket cannot be used: TKT-A1B2C3D4"));
    }
}