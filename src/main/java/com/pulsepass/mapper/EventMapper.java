package com.pulsepass.mapper;

import com.pulsepass.domain.Event;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper de MapStruct: convierte entidades JPA en DTOs de respuesta. Es solo una INTERFAZ: durante la compilación MapStruct (procesador de anotaciones configurado en el pom.xml) genera la clase que la implementa, {@code componentModel = "spring"} la registra como bean para poder inyectarla. Los campos con el mismo nombre se copian solos; {@code @Mapping} cubre los casos con nombre o ruta distinta. Se invoca desde los servicios, dentro de la transacción: los mappers que recorren relaciones LAZY (Event y Ticket) lo necesitan, ya que {@code spring.jpa.open-in-view=false}.
 *
 * <p>Event -> EventResponse (detalle) y EventSummaryResponse (resumen). {@code uses = ArtistMapper.class}
 * le indica que, al convertir la colección de artistas del evento, delegue cada elemento en {@code ArtistMapper}.</p>
 */
@Mapper(componentModel = "spring", uses = ArtistMapper.class)
public interface EventMapper {

    // Aplana el recinto: el destino venueCode/venueName se llena con event.getVenue().getCode()/getName().
    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    EventResponse toResponse(Event event);

    @Mapping(target = "venueName", source = "venue.name")
    // Versión resumida: el resto de campos (id, eventCode, name, category, status, eventDate) coinciden por nombre.
    EventSummaryResponse toSummary(Event event);
}
