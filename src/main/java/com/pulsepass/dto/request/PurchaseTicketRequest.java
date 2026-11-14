package com.pulsepass.dto.request;

import com.pulsepass.domain.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de ENTRADA: representa el JSON que el cliente envía. Es un {@code record} (inmutable, con constructor y accesores generados). Las anotaciones de Bean Validation ({@code @NotBlank}: no nulo ni vacío; {@code @NotNull}: no nulo; {@code @Size}: longitud; {@code @Email}: formato) se ejecutan cuando el controlador lo recibe con {@code @Valid}; si alguna falla, la respuesta es 400 con el detalle por campo en {@code details}. Son validaciones de FORMA: las reglas de negocio las valida el servicio.
 *
 * <p>Datos para comprar un ticket: quién (email), para qué evento (código) y de qué tipo.
 * No incluye el precio: lo calcula el servidor a partir del tipo (ver {@code TicketPriceCalculator}), por lo que
 * el cliente no puede elegir cuánto paga.</p>
 */
public record PurchaseTicketRequest(

        @NotBlank(message = "User email is required")
        @Email(message = "Email format is invalid")
        String userEmail,

        @NotBlank(message = "Event code is required")
        String eventCode,

        @NotNull(message = "Ticket type is required")
        TicketType type
) {
}
