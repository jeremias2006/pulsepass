package com.pulsepass.dto.response;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de SALIDA: es el JSON que se devuelve al cliente. Es un {@code record} inmutable. Se usa un DTO en lugar de devolver la entidad JPA para no exponer la estructura interna de la base de datos ni arrastrar relaciones LAZY al serializar. Se construye con MapStruct (paquete {@code mapper}).
 *
 * <p>Detalle completo de un evento. Aplana datos del recinto ({@code venueCode}, {@code venueName})
 * y trae la lista de artistas. Para listados se usa la versión corta {@link EventSummaryResponse}.</p>
 */
public record EventResponse(
        Long id,
        String eventCode,
        String name,
        String description,
        EventCategory category,
        EventStatus status,
        LocalDate eventDate,
        Integer minimumAge,
        String venueCode,
        String venueName,
        List<ArtistResponse> artists
) {
}
