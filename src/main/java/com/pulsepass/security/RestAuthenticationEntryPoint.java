package com.pulsepass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 401 (Unauthorized) en formato JSON cuando alguien intenta acceder a una ruta protegida
 * sin estar autenticado (sin token, o con un token inválido o vencido).
 *
 * <p>Spring Security invoca este componente por sí mismo; está registrado en
 * {@code SecurityConfig} mediante {@code exceptionHandling}. Diferencia con
 * {@code RestAccessDeniedHandler}: 401 = "no sé quién eres", 403 = "sé quién eres, pero no tienes permiso".</p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    private final SecurityErrorWriter errorWriter;

    public RestAuthenticationEntryPoint(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("auth_required method={} path={}", request.getMethod(), request.getRequestURI());
        // El estándar HTTP exige indicar con qué esquema autenticarse al responder 401: aquí, "Bearer" (token).
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        // Mensaje único y genérico: no distingue entre falta de token, token vencido o inválido.
        errorWriter.write(response, HttpStatus.UNAUTHORIZED,
                "Autenticación requerida o credenciales inválidas");
    }
}
