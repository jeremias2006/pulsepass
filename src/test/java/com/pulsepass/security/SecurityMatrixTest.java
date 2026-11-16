package com.pulsepass.security;

import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.service.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Matriz de seguridad": comprueba las reglas POR URL de SecurityConfig para cada ruta y cada tipo de llamante (anónimo, USER,
 * STAFF, ADMIN). Es la primera capa de autorización; las reglas finas (propietario del ticket, etc.) las cubre otra parte.
 *
 * <ul>
 *   <li>{@code @WebMvcTest}: levanta solo la capa web (controladores, MVC y seguridad), sin base de datos.</li>
 *   <li>{@code @Import}: agrega SecurityConfig y los manejadores de errores 401/403, que @WebMvcTest no carga por sí solo.</li>
 *   <li>{@code @MockitoBean}: reemplaza cada servicio por un doble, de modo que no se ejecuta lógica de negocio. JwtService y
 *       CustomUserDetailsService también, porque SecurityConfig los usa para construir el filtro JWT.</li>
 *   <li>{@code user("x").roles("USER")}: simula un usuario ya autenticado con ese rol; el filtro JWT no sobrescribe una autenticación existente.</li>
 * </ul>
 */
@WebMvcTest   // carga TODOS los controllers
@Import({SecurityConfig.class, SecurityErrorWriter.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, GlobalExceptionHandler.class})
class SecurityMatrixTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean VenueService venueService;
    @MockitoBean EventService eventService;
    @MockitoBean ArtistService artistService;
    @MockitoBean UserService userService;
    @MockitoBean TicketService ticketService;
    @MockitoBean AuthService authService;
    @MockitoBean AuthorizationService authorizationService;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    // Una ruta a probar: verbo HTTP + ruta. toString da un nombre legible a cada caso en el reporte de pruebas.
    record Route(HttpMethod method, String path) {
        @Override public String toString() { return method + " " + path; }
    }

    // Rutas exclusivas de ADMIN.
    static Stream<Route> adminOnly() {
        return Stream.of(
                new Route(HttpMethod.POST, "/api/events"),
                new Route(HttpMethod.PATCH, "/api/events/CMF-2026/publish"),
                new Route(HttpMethod.POST, "/api/events/CMF-2026/artists/1"));
    }

    // Rutas para STAFF o ADMIN.
    static Stream<Route> staffOrAdmin() {
        return Stream.of(
                new Route(HttpMethod.PATCH, "/api/tickets/TKT-1/use"),
                new Route(HttpMethod.GET, "/api/events/CMF-2026/tickets/paid"));
    }

    // Rutas que solo exigen estar autenticado (el control fino por propietario lo hace @PreAuthorize en el servicio).
    static Stream<Route> authenticatedOnly() {
        return Stream.of(
                new Route(HttpMethod.GET, "/api/tickets/TKT-1"),
                new Route(HttpMethod.GET, "/api/tickets/by-user?email=a@mail.com"),
                new Route(HttpMethod.PATCH, "/api/tickets/TKT-1/cancel"),
                new Route(HttpMethod.GET, "/api/users/by-email?email=a@mail.com"),
                new Route(HttpMethod.GET, "/api/users/by-username?username=andrea"),
                new Route(HttpMethod.GET, "/api/auth/me"));
    }

    // Rutas públicas: no requieren token.
    static Stream<Route> publicRoutes() {
        return Stream.of(
                new Route(HttpMethod.GET, "/api/venues/active"),
                new Route(HttpMethod.GET, "/api/venues/VEN-SMR-01"),
                new Route(HttpMethod.GET, "/api/events/published"),
                new Route(HttpMethod.GET, "/api/events/by-artist?stageName=Solar Beat"),
                new Route(HttpMethod.GET, "/api/events/CMF-2026"),
                new Route(HttpMethod.GET, "/api/artists/active"),
                new Route(HttpMethod.GET, "/api/artists/by-stage-name?stageName=Solar Beat"),
                new Route(HttpMethod.GET, "/api/artists/1"),
                new Route(HttpMethod.POST, "/api/users"),
                new Route(HttpMethod.POST, "/api/auth/login"));
    }

    private MockHttpServletRequestBuilder build(Route r) {
        return request(r.method(), r.path())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
    }

    // Ejecuta la petición y devuelve solo el código HTTP.
    private int statusOf(MockHttpServletRequestBuilder b) throws Exception {
        return mockMvc.perform(b).andReturn().getResponse().getStatus();
    }

    // ---- 401 anónimo ----
    // Sin token, toda ruta protegida responde 401 con el JSON estándar de error (status, error, message y timestamp).
    @ParameterizedTest(name = "anónimo → 401: {0}")
    @MethodSource({"adminOnly", "staffOrAdmin", "authenticatedOnly"})
    void anonymousGets401WithErrorResponse(Route r) throws Exception {
        mockMvc.perform(build(r))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ---- 403 rol insuficiente ----
    // Un USER en rutas de ADMIN o de STAFF/ADMIN: 403 (identificado, pero sin permiso).
    @ParameterizedTest(name = "USER → 403: {0}")
    @MethodSource({"adminOnly", "staffOrAdmin"})
    void userIsForbidden(Route r) throws Exception {
        mockMvc.perform(build(r).with(user("andrea").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // Un STAFF tampoco puede usar las rutas exclusivas de ADMIN: 403.
    @ParameterizedTest(name = "STAFF → 403 en rutas ADMIN: {0}")
    @MethodSource("adminOnly")
    void staffIsForbiddenOnAdminOnly(Route r) throws Exception {
        mockMvc.perform(build(r).with(user("laura").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    // ADMIN no es rechazado por seguridad: el código NO debe ser 401 ni 403 (otro, como 400 o 404, es aceptable porque los
    // servicios son dobles y el cuerpo es "{}"). Lo que se prueba es que la regla de URL lo deja pasar.
    @ParameterizedTest(name = "ADMIN permitido: {0}")
    @MethodSource({"adminOnly", "staffOrAdmin"})
    void adminIsNotRejectedBySecurity(Route r) throws Exception {
        assertThat(statusOf(build(r).with(user("admin").roles("ADMIN")))).isNotIn(401, 403);
    }

    // STAFF pasa la regla de URL en las rutas de STAFF/ADMIN (no 401 ni 403).
    @ParameterizedTest(name = "STAFF permitido: {0}")
    @MethodSource("staffOrAdmin")
    void staffIsNotRejectedBySecurity(Route r) throws Exception {
        assertThat(statusOf(build(r).with(user("laura").roles("STAFF")))).isNotIn(401, 403);
    }

    // ---- públicas ----
    // Anónimo en rutas públicas: nunca 401/403 (un 400 por cuerpo vacío en un POST es válido aquí).
    @ParameterizedTest(name = "anónimo permitido: {0}")
    @MethodSource("publicRoutes")
    void publicRoutesAreOpen(Route r) throws Exception {
        assertThat(statusOf(build(r))).isNotIn(401, 403);   // 400 por body vacío en POST es válido aquí
    }

    // Comprar un ticket exige el rol USER: anónimo 401, STAFF 403 y USER pasa la regla de URL.
    @org.junit.jupiter.api.Test
    void purchaseRequiresUserRole() throws Exception {
        Route purchase = new Route(HttpMethod.POST, "/api/tickets");

        mockMvc.perform(build(purchase)).andExpect(status().isUnauthorized());
        mockMvc.perform(build(purchase).with(user("laura").roles("STAFF"))).andExpect(status().isForbidden());
        assertThat(statusOf(build(purchase).with(user("andrea").roles("USER")))).isNotIn(401, 403);
    }
}