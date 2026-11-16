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

/**
 * Prueba de la CAPA WEB (HTTP) del controlador, sin base de datos ni lógica de negocio real. Se envían peticiones HTTP simuladas
 * con {@code MockMvc} y se comprueba la ruta, el código de respuesta, el JSON devuelto, las validaciones ({@code @Valid}) y cómo
 * {@code GlobalExceptionHandler} traduce las excepciones.
 *
 * <ul>
 *   <li>{@code @WebMvcTest}: levanta solo Spring MVC y seguridad, y únicamente el controlador indicado.</li>
 *   <li>{@code @Import}: agrega lo que @WebMvcTest no carga solo: GlobalExceptionHandler, SecurityConfig y los manejadores de
 *       errores 401/403.</li>
 *   <li>{@code @MockitoBean}: reemplaza el servicio por un doble. {@code when(...).thenReturn(...)} o {@code thenThrow(...)} define
 *       lo que "responde" el servicio, así solo se prueba el controlador. JwtService, CustomUserDetailsService y
 *       AuthorizationService también se simulan: son dependencias de la seguridad y así el contexto arranca sin el resto de la aplicación.</li>
 *   <li>{@code jsonPath("$.campo")}: lee un campo del JSON de respuesta. {@code verify(servicio, never())} demuestra que el
 *       controlador rechazó la petición ANTES de llegar al servicio.</li>
 * </ul>
 *
 * /login es público; /me exige estar autenticado. AuthService es el doble que simula el inicio de sesión.
 */
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

    // Cuerpo JSON de login válido, reutilizado por varias pruebas (bloque de texto """ ... """).
    private static final String LOGIN_BODY = """
            {"identifier":"andrea","password":"UnaFraseSeguraLarga2026!"}""";

    // ---------------------------------------------------------------- login
    // Login correcto: 200 con accessToken, tokenType "Bearer" y expiresIn 900 (15 minutos).
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

    // Identificador y contraseña vacíos: 400 con un error por cada campo en details. El servicio no se llama.
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

    // Cuerpo "{}" (faltan campos obligatorios): 400.
    @Test
    void loginWithMissingBodyFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // Credenciales malas: 401 con el JSON estándar. Comprueba además que la respuesta NO contiene la contraseña ni el nombre
    // de la excepción interna.
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
    // /me sin autenticación: 401, y el servicio nunca se invoca.
    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(authService, never()).me(any());
    }

    // with(user(principal)) simula un usuario autenticado con un SecurityPrincipal real. /me devuelve sus datos y roles
    // y la respuesta NO contiene ni contraseña ni hash.
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
    // Regresión: un AccessDeniedException lanzado dentro del controlador o servicio (lo que hace @PreAuthorize) debe
    // responder 403 en JSON y no un 500. Lo resuelve el handler de AccessDeniedException de GlobalExceptionHandler.
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