package com.pulsepass.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    // El contexto de seguridad es estático: hay que limpiarlo tras cada test
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------ sin token

    @Test
    void withoutAuthorizationHeader_continuesWithEmptyContext() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();          // la cadena continuó
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void nonBearerScheme_isIgnored() throws Exception {
        request.addHeader("Authorization", "Basic YW5kcmVhOnNlY3JldA==");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void emptyBearerToken_isIgnored() throws Exception {
        request.addHeader("Authorization", "Bearer   ");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // -------------------------------------------------------- token válido

    @Test
    void validBearer_setsAuthenticationWithPrincipal() throws Exception {
        SecurityPrincipal principal = principal(7L, true);
        when(jwtService.extractUserId("good-token")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(principal);
        request.addHeader("Authorization", "Bearer good-token");

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(principal);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void bearerSchemeIsCaseInsensitive() throws Exception {
        SecurityPrincipal principal = principal(7L, true);
        when(jwtService.extractUserId("good-token")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(principal);
        request.addHeader("Authorization", "bearer good-token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    // ----------------------------------------------------- token no válido

    @Test
    void invalidBearer_continuesWithoutAuthenticationAndWithoutException() throws Exception {
        when(jwtService.extractUserId("bad-token")).thenThrow(new InvalidTokenException("Invalid token"));
        request.addHeader("Authorization", "Bearer bad-token");

        filter.doFilter(request, response, chain);   // si lanzara excepción, el test fallaría aquí

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void unexpectedRuntimeException_neverBecomesHttp500() throws Exception {
        when(jwtService.extractUserId("weird-token")).thenThrow(new IllegalArgumentException("boom"));
        request.addHeader("Authorization", "Bearer weird-token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void validTokenOfUnknownUser_isNotAuthenticated() throws Exception {
        when(jwtService.extractUserId("good-token")).thenReturn(99L);
        when(userDetailsService.loadUserById(99L)).thenThrow(new UsernameNotFoundException("Credentials not found"));
        request.addHeader("Authorization", "Bearer good-token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void inactiveUser_isNotAuthenticated() throws Exception {
        // UC-SEC-10 / SEC011: el token sigue siendo válido, pero la cuenta fue desactivada
        when(jwtService.extractUserId("good-token")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(principal(7L, false));
        request.addHeader("Authorization", "Bearer good-token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    // ------------------------------------------------- casos sospechosos

    @Test
    void twoAuthorizationHeaders_areNotAuthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer token-uno");
        request.addHeader("Authorization", "Bearer token-dos");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void existingAuthentication_isNotOverwritten() throws Exception {
        Authentication existing = UsernamePasswordAuthenticationToken.authenticated(
                "ya-autenticado", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer good-token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void tokenInQueryString_isIgnored() throws Exception {
        // FR-SECAUTH-007: el JWT solo se acepta por el header Authorization
        request.setParameter("access_token", "good-token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // ----------------------------------------------------------------- helpers

    private SecurityPrincipal principal(Long id, boolean enabled) {
        return new SecurityPrincipal(id, "andrea", "andrea@mail.com", "hash", enabled,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}