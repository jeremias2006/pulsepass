package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Asigna un identificador único a cada petición para poder seguirla en los logs.
 *
 * <p>El id se guarda en el {@code MDC} (contexto de diagnóstico del logging): {@code application.properties}
 * configura el patrón de log con {@code %X{requestId}}, por lo que cada línea de log de esa petición
 * muestra su id. También se devuelve al cliente en la cabecera {@code X-Request-Id}, para que pueda
 * citarlo si reporta un problema.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // antes de la cadena de Spring Security
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Se genera siempre en el servidor (no se confía en un header del cliente: log injection)
        String requestId = UUID.randomUUID().toString();
        // Disponible para todo el logging de esta petición (hilo actual).
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Siempre se limpia, incluso si hubo excepción: los hilos del servidor se reutilizan y el id
            // de esta petición no debe "filtrarse" a la siguiente.
            MDC.remove("requestId");
        }
    }
}