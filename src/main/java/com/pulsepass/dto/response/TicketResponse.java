package com.pulsepass.dto.response;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de SALIDA: es el JSON que se devuelve al cliente. Es un {@code record} inmutable. Se usa un DTO en lugar de devolver la entidad JPA para no exponer la estructura interna de la base de datos ni arrastrar relaciones LAZY al serializar. Se construye con MapStruct (paquete {@code mapper}).
 *
 * <p>Datos de un ticket. Aplana información de las relaciones: el email del usuario
 * ({@code userEmail}) y el código y nombre del evento ({@code eventCode}, {@code eventName}).
 * El precio es un {@code BigDecimal} (dinero exacto, sin errores de coma flotante).</p>
 */
public record TicketResponse(
        Long id,
        String ticketCode,
        TicketType type,
        BigDecimal price,
        TicketStatus status,
        LocalDateTime purchaseDate,
        String userEmail,
        String eventCode,
        String eventName
) {
}
