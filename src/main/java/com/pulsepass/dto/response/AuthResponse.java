package com.pulsepass.dto.response;

/**
 * DTO de SALIDA: es el JSON que se devuelve al cliente. Es un {@code record} inmutable. Se usa un DTO en lugar de devolver la entidad JPA para no exponer la estructura interna de la base de datos ni arrastrar relaciones LAZY al serializar. Se construye con MapStruct (paquete {@code mapper}).
 *
 * <p>Resultado del login: el token JWT, su tipo y cuántos segundos de vigencia tiene. El cliente
 * debe enviar el token en la cabecera {@code Authorization: Bearer <token>}. {@code toString} oculta el token.</p>
 */
public record AuthResponse(
        String accessToken,
        String tokenType,   // "Bearer"
        long expiresIn      // segundos (900)
) {

    // Sobrescribe toString para enmascarar el token si alguien imprime o registra el objeto.
    @Override
    public String toString() {
        return "AuthResponse[accessToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}