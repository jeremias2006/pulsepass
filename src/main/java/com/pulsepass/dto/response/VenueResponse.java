package com.pulsepass.dto.response;

/** DTO de SALIDA: es el JSON que se devuelve al cliente. Es un {@code record} inmutable. Se usa un DTO en lugar de devolver la entidad JPA para no exponer la estructura interna de la base de datos ni arrastrar relaciones LAZY al serializar. Se construye con MapStruct (paquete {@code mapper}). Datos de un recinto (incluida su capacidad máxima de asistentes). */
public record VenueResponse(
        Long id,
        String code,
        String name,
        String city,
        Integer capacity,
        boolean active
) {
}
