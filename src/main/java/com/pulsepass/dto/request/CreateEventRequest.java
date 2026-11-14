package com.pulsepass.dto.request;

import com.pulsepass.domain.EventCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de ENTRADA: representa el JSON que el cliente envía. Es un {@code record} (inmutable, con constructor y accesores generados). Las anotaciones de Bean Validation ({@code @NotBlank}: no nulo ni vacío; {@code @NotNull}: no nulo; {@code @Size}: longitud; {@code @Email}: formato) se ejecutan cuando el controlador lo recibe con {@code @Valid}; si alguna falla, la respuesta es 400 con el detalle por campo en {@code details}. Son validaciones de FORMA: las reglas de negocio las valida el servicio.
 *
 * <p>Datos para crear un evento. No tiene campo de estado: todo evento nace como DRAFT (BR-EVENT-005).
 * El recinto se indica por su código ({@code venueCode}), no por id.</p>
 */
public record CreateEventRequest(

        @NotBlank(message = "Event code is required")
        @Size(max = 30, message = "Event code must not exceed 30 characters")
        String eventCode,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Category is required")
        EventCategory category,

        @NotNull(message = "Event date is required")
        LocalDate eventDate,

        // 0 = sin restricción de edad (ver BR-EVENT-006 y BR-TICKET-006).
        @NotNull(message = "Minimum age is required")
        @Min(value = 0, message = "Minimum age must be zero or greater")
        Integer minimumAge,

        @NotBlank(message = "Venue code is required")
        @Size(max = 20, message = "Venue code must not exceed 20 characters")
        String venueCode
) {
}
