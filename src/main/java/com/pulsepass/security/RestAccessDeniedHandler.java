package com.pulsepass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 403 (Forbidden) en formato JSON cuando un usuario SÍ autenticado intenta algo para lo que
 * no tiene permiso (por ejemplo, un USER llamando a una ruta exclusiva de ADMIN).
 *
 * <p>Atiende las reglas por URL de {@code SecurityConfig}. Las denegaciones de {@code @PreAuthorize}
 * (a nivel de método en los servicios) llegan como excepción y las resuelve {@code GlobalExceptionHandler},
 * que también tiene un {@code @ExceptionHandler(AccessDeniedException.class)}; en ambos casos el
 * cliente recibe el mismo 403 en JSON.</p>
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final SecurityErrorWriter errorWriter;

    public RestAccessDeniedHandler(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        // Se registra en el log quién intentó qué (solo el nombre de usuario, nunca datos sensibles).
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.warn("access_denied user={} method={} path={}",
                auth != null ? auth.getName() : "anonymous", request.getMethod(), request.getRequestURI());
        errorWriter.write(response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
    }
}