package com.pulsepass.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Se lanza cuando un token JWT no es aceptable (firma incorrecta, vencido, malformado, sin los
 * claims obligatorios, etc.). Extiende {@code AuthenticationException} para que Spring Security
 * la trate como un fallo de autenticación (respuesta 401) y no como un error del servidor.
 * El mensaje es deliberadamente genérico: no detalla al cliente qué falló exactamente.
 */
public class InvalidTokenException extends AuthenticationException {

    // Para fallos detectados por nuestras propias validaciones (algoritmo, claims faltantes, iat).
    public InvalidTokenException(String message) {
        super(message);
    }

    // Envuelve la excepción original de la librería JWT para conservarla como causa (útil al depurar).
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}