package com.pulsepass.exception;

/**
 * Se lanza cuando se intenta crear algo que debe ser único y ya existe (username, email, eventCode).
 * {@code GlobalExceptionHandler} la convierte en una respuesta <b>409 Conflict</b>.
 * Es no verificada ({@code RuntimeException}): provoca rollback de la transacción.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
