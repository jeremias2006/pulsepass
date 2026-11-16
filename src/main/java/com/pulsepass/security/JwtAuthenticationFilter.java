package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro que autentica cada petición a partir del token JWT de la cabecera
 * {@code Authorization: Bearer <token>}.
 *
 * <p>Extiende {@code OncePerRequestFilter}: se ejecuta una sola vez por petición. Se coloca antes de
 * {@code UsernamePasswordAuthenticationFilter} en la cadena (ver {@code SecurityConfig}).</p>
 *
 * <p>Flujo: leer la cabecera -> validar el token ({@link JwtService}) -> cargar al usuario actual de
 * la base ({@link CustomUserDetailsService}) -> si todo es correcto, dejar la autenticación en el
 * {@code SecurityContext}. <b>Si algo falla, el filtro NO responde con error</b>: simplemente deja la
 * petición sin autenticar y continúa la cadena. Más adelante Spring decide: en una ruta pública la
 * petición pasa; en una ruta protegida {@code RestAuthenticationEntryPoint} responde 401.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    // Prefijo esperado de la cabecera Authorization (se compara sin distinguir mayúsculas).
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;                    // valida el token y extrae el id
    private final CustomUserDetailsService userDetailsService; // carga usuario, estado y roles actuales

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Este bloque decide SI se intenta autenticar con el token. Condiciones:
        // 1) Ya autenticado (p. ej. @WithMockUser en tests): no se sobrescribe.
        // 2) Más de un header Authorization: sospechoso, no se autentica.
        if (SecurityContextHolder.getContext().getAuthentication() == null
                && Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION)).size() <= 1) {

            // El header debe empezar por "Bearer " (regionMatches con ignoreCase=true: no importa mayúsculas).
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                // Lo que sigue al prefijo es el token en sí.
                String token = header.substring(BEARER_PREFIX.length()).trim();
                if (!token.isEmpty()) {
                    tryAuthenticate(token, request);
                }
            }
        }

        // Fuera del try: los errores de la cadena/negocio NO se tragan aquí.
        // Siempre se continúa la cadena, haya quedado autenticada la petición o no.
        filterChain.doFilter(request, response);
    }

    /** Intenta autenticar con el token; ante cualquier problema deja la petición sin autenticar. */
    private void tryAuthenticate(String token, HttpServletRequest request) {
        try {
            // 1) Valida firma, emisor, vencimiento y claims; devuelve el id del usuario.
            Long userId = jwtService.extractUserId(token);
            // 2) Consulta a la base el usuario con su estado y roles ACTUALES (no los del momento del login).
            SecurityPrincipal principal = userDetailsService.loadUserById(userId);

            // Un token válido de un usuario desactivado (active = false) tampoco autentica.
            if (!principal.isEnabled()) {              // SEC011 / UC-SEC-10
                log.warn("jwt_rejected reason=inactive_user userId={}", userId);
                return;
            }

            // Objeto "autenticado" de Spring Security: principal + (sin credenciales, ya no hacen falta) + roles.
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Se crea un contexto nuevo y se asigna completo, en lugar de modificar el que ya existe
            // (práctica recomendada por Spring Security). Desde aquí, la petición cuenta como autenticada.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch (AuthenticationException ex) {          // InvalidTokenException, UsernameNotFoundException
            // El orden de los catch importa: AuthenticationException es una RuntimeException más específica,
            // por eso va primero. Token inválido o usuario inexistente: se limpia el contexto y NO se autentica.
            SecurityContextHolder.clearContext();
            log.warn("jwt_rejected reason={}", ex.getClass().getSimpleName());
        } catch (RuntimeException ex) {                 // NFR-SEC002: jamás HTTP 500 por un Bearer raro
            SecurityContextHolder.clearContext();
            log.warn("jwt_rejected reason=unexpected error={}", ex.getClass().getSimpleName());
        }
    }
}