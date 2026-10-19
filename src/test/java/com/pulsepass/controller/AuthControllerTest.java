package com.pulsepass.controller;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.security.SecurityErrorWriter;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SecurityErrorWriter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // SecurityConfig los necesita para construir la cadena de filtros
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private AuthorizationService authorizationService;

    private static final String LOGIN_BODY = """
            {"identifier":"andrea","password":"UnaFraseSeguraLarga2026!"}""";

    // ---------------------------------------------------------------- login
    @Test
    void loginReturns200WithBearerToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-ficticio", "Bearer", 900));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-ficticio"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void loginWithBlankFieldsReturns400WithDetails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.identifier").exists())
                .andExpect(jsonPath("$.details.password").exists());

        verify(authService, never()).login(any());
    }

    @Test
    void loginWithMissingBodyFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void badCredentialsReturnGeneric401WithErrorResponse() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("UnaFraseSeguraLarga2026!"))))
                .andExpect(content().string(not(containsString("BadCredentialsException"))));
    }

    // ---------------------------------------------------------------- me
    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(authService, never()).me(any());
    }

    @Test
    void meWithPrincipalReturns200WithoutSecrets() throws Exception {
        SecurityPrincipal principal = new SecurityPrincipal(7L, "andrea", "andrea@mail.com",
                "hash-que-no-debe-salir", true, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authService.me(any(SecurityPrincipal.class)))
                .thenReturn(new AuthenticatedUserResponse(7L, "andrea", "andrea@mail.com", List.of("USER")));

        mockMvc.perform(get("/api/auth/me").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("hash-que-no-debe-salir"))));
    }

    // ------------------------------------------- regresión del Paso 15 (403 ≠ 500)
    @Test
    void accessDeniedThrownInsideControllerReturns403NotHttp500() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AccessDeniedException("Denied"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(content().string(not(containsString("AccessDeniedException"))));
    }
}