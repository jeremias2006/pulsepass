package com.pulsepass.dto.response;

import java.util.List;

/** DTO de SALIDA: es el JSON que se devuelve al cliente. Es un {@code record} inmutable. Se usa un DTO en lugar de devolver la entidad JPA para no exponer la estructura interna de la base de datos ni arrastrar relaciones LAZY al serializar. Se construye con MapStruct (paquete {@code mapper}). Respuesta de {@code GET /api/auth/me}: quién es el usuario autenticado y con qué roles. */
public record AuthenticatedUserResponse(
        Long id,
        String username,
        String email,
        List<String> roles   // ["USER"], ["ADMIN"]...
) {
}