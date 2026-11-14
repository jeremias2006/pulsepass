package com.pulsepass.mapper;

import com.pulsepass.domain.Ticket;
import com.pulsepass.dto.response.TicketResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper de MapStruct: convierte entidades JPA en DTOs de respuesta. Es solo una INTERFAZ: durante la compilación MapStruct (procesador de anotaciones configurado en el pom.xml) genera la clase que la implementa, {@code componentModel = "spring"} la registra como bean para poder inyectarla. Los campos con el mismo nombre se copian solos; {@code @Mapping} cubre los casos con nombre o ruta distinta. Se invoca desde los servicios, dentro de la transacción: los mappers que recorren relaciones LAZY (Event y Ticket) lo necesitan, ya que {@code spring.jpa.open-in-view=false}.
 *
 * <p>Ticket -> TicketResponse: aplana el usuario y el evento relacionados.</p>
 */
@Mapper(componentModel = "spring")
public interface TicketMapper {

    // Los datos del usuario y del evento viven en entidades relacionadas; "source" indica la ruta para llegar a ellos.
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "eventCode", source = "event.eventCode")
    @Mapping(target = "eventName", source = "event.name")
    TicketResponse toResponse(Ticket ticket);
}
