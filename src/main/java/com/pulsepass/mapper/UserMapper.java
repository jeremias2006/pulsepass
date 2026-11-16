package com.pulsepass.mapper;

import com.pulsepass.domain.User;
import com.pulsepass.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper de MapStruct: convierte entidades JPA en DTOs de respuesta. Es solo una INTERFAZ: durante la compilación MapStruct (procesador de anotaciones configurado en el pom.xml) genera la clase que la implementa, {@code componentModel = "spring"} la registra como bean para poder inyectarla. Los campos con el mismo nombre se copian solos; {@code @Mapping} cubre los casos con nombre o ruta distinta. Se invoca desde los servicios, dentro de la transacción: los mappers que recorren relaciones LAZY (Event y Ticket) lo necesitan, ya que {@code spring.jpa.open-in-view=false}.
 *
 * <p>User -> UserResponse: aplana los datos del perfil (UserProfile) dentro de la respuesta.
 * La respuesta no tiene contraseña: esa información no está en {@code User}.</p>
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    // Los datos personales están en user.getProfile(); se copian al nivel superior de la respuesta.
    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "phone", source = "profile.phone")
    @Mapping(target = "city", source = "profile.city")
    @Mapping(target = "birthDate", source = "profile.birthDate")
    UserResponse toResponse(User user);
}
