package com.pulsepass.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura ÚNICA de todos los errores de la API (la arman {@code GlobalExceptionHandler} y
 * {@code SecurityErrorWriter}), de modo que el cliente siempre los lee igual.
 *
 * <ul>
 *   <li>{@code timestamp}: cuándo ocurrió.</li>
 *   <li>{@code status}: código HTTP numérico (404, 409...).</li>
 *   <li>{@code error}: frase estándar del código ("Not Found", "Conflict"...).</li>
 *   <li>{@code message}: explicación para humanos.</li>
 *   <li>{@code details}: mapa campo -> motivo (p. ej. errores de validación); vacío si no aplica.</li>
 * </ul>
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
}
