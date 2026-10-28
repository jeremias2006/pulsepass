package com.pulsepass.security;

import com.pulsepass.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Escribe directamente en la respuesta HTTP el mismo JSON de error ({@code ErrorResponse}) que usa
 * el resto de la API.
 *
 * <p>Hace falta porque los errores 401 y 403 de las reglas por URL ocurren dentro de la cadena de
 * filtros de Spring Security, ANTES de llegar a los controladores; allí {@code GlobalExceptionHandler}
 * (que solo actúa sobre excepciones de los controladores) no participa. Lo usan
 * {@code RestAuthenticationEntryPoint} y {@code RestAccessDeniedHandler}.</p>
 */
@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;   // convierte el objeto ErrorResponse a JSON

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;   // la respuesta se emite una sola vez
        }
        // Misma estructura que los demás errores de la API: fecha, código, motivo, mensaje y detalles (vacío).
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                Map.of());

        // Se fijan código HTTP, tipo de contenido JSON y codificación UTF-8 (para las tildes de los mensajes).
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
