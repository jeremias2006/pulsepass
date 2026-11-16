package com.pulsepass.exception;

/**
 * Se lanza cuando una operación viola una regla de negocio (los códigos BR-* de los servicios):
 * evento no publicado, edad insuficiente, cupo agotado, ticket que no se puede cancelar, etc.
 * {@code GlobalExceptionHandler} la convierte en una respuesta <b>409 Conflict</b>; el mensaje explica
 * qué regla falló. Es no verificada ({@code RuntimeException}): provoca rollback de la transacción.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
