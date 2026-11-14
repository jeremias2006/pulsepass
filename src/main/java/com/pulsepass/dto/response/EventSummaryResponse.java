package com.pulsepass.dto.response;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;

import java.time.LocalDate;

/** DTO de SALIDA: es el JSON que se devuelve al cliente. Es un {@code record} inmutable. Se usa un DTO en lugar de devolver la entidad JPA para no exponer la estructura interna de la base de datos ni arrastrar relaciones LAZY al serializar. Se construye con MapStruct (paquete {@code mapper}). Versión resumida de un evento para listados: sin descripción, edad mínima ni artistas. */
public record EventSummaryResponse(
        Long id,
        String eventCode,
        String name,
        EventCategory category,
        EventStatus status,
        LocalDate eventDate,
        String venueName
) {
}
