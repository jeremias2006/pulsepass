package com.pulsepass.dto.response;

import java.time.LocalDate;

/**
 * DTO de SALIDA: es el JSON que se devuelve al cliente. Es un {@code record} inmutable. Se usa un DTO en lugar de devolver la entidad JPA para no exponer la estructura interna de la base de datos ni arrastrar relaciones LAZY al serializar. Se construye con MapStruct (paquete {@code mapper}).
 *
 * <p>Datos de un usuario con los de su perfil aplanados. NO incluye contraseña ni hash: esos viven en
 * otra entidad ({@code UserCredential}) que nunca se expone.</p>
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        boolean active,
        String firstName,
        String lastName,
        String phone,
        String city,
        LocalDate birthDate
) {
}
