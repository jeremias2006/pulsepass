package com.pulsepass.exception;

/**
 * Se lanza cuando un recurso buscado no existe (usuario, evento, ticket, artista, recinto...).
 * {@code GlobalExceptionHandler} la convierte en una respuesta <b>404 Not Found</b>.
 *
 * <p>Extiende {@code RuntimeException} (no verificada): no obliga a declarar {@code throws} y, por
 * defecto, Spring revierte la transacción ({@code @Transactional}) cuando se lanza.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
