package com.pulsepass.mapper;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import org.mapstruct.Mapper;

/**
 * Mapper de MapStruct: convierte entidades JPA en DTOs de respuesta. Es solo una INTERFAZ: durante la compilación MapStruct (procesador de anotaciones configurado en el pom.xml) genera la clase que la implementa, {@code componentModel = "spring"} la registra como bean para poder inyectarla. Los campos con el mismo nombre se copian solos; {@code @Mapping} cubre los casos con nombre o ruta distinta. Se invoca desde los servicios, dentro de la transacción: los mappers que recorren relaciones LAZY (Event y Ticket) lo necesitan, ya que {@code spring.jpa.open-in-view=false}.
 *
 * <p>Artist -> ArtistResponse. Todos los campos coinciden por nombre, por lo que no necesita {@code @Mapping}.</p>
 */
@Mapper(componentModel = "spring")
public interface ArtistMapper {

    ArtistResponse toResponse(Artist artist);
}
