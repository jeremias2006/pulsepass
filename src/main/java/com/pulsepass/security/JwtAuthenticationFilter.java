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

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1) Ya autenticado (p. ej. @WithMockUser en tests): no se sobrescribe.
        // 2) Más de un header Authorization: sospechoso, no se autentica.
        if (SecurityContextHolder.getContext().getAuthentication() == null
                && Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION)).size() <= 1) {

            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                String token = header.substring(BEARER_PREFIX.length()).trim();
                if (!token.isEmpty()) {
                    tryAuthenticate(token, request);
                }
            }
        }

        // Fuera del try: los errores de la cadena/negocio NO se tragan aquí.
        filterChain.doFilter(request, response);
    }

    private void tryAuthenticate(String token, HttpServletRequest request) {
        try {
            Long userId = jwtService.extractUserId(token);
            SecurityPrincipal principal = userDetailsService.loadUserById(userId);

            if (!principal.isEnabled()) {              // SEC011 / UC-SEC-10
                log.warn("jwt_rejected reason=inactive_user userId={}", userId);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch (AuthenticationException ex) {          // InvalidTokenException, UsernameNotFoundException
            SecurityContextHolder.clearContext();
            log.warn("jwt_rejected reason={}", ex.getClass().getSimpleName());
        } catch (RuntimeException ex) {                 // NFR-SEC002: jamás HTTP 500 por un Bearer raro
            SecurityContextHolder.clearContext();
            log.warn("jwt_rejected reason=unexpected error={}", ex.getClass().getSimpleName());
        }
    }
}