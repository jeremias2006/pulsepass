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

    record Route(HttpMethod method, String path) {
        @Override public String toString() { return method + " " + path; }
    }

    static Stream<Route> adminOnly() {
        return Stream.of(
                new Route(HttpMethod.POST, "/api/events"),
                new Route(HttpMethod.PATCH, "/api/events/CMF-2026/publish"),
                new Route(HttpMethod.POST, "/api/events/CMF-2026/artists/1"));
    }

    static Stream<Route> staffOrAdmin() {
        return Stream.of(
                new Route(HttpMethod.PATCH, "/api/tickets/TKT-1/use"),
                new Route(HttpMethod.GET, "/api/events/CMF-2026/tickets/paid"));
    }

    static Stream<Route> authenticatedOnly() {
        return Stream.of(
                new Route(HttpMethod.GET, "/api/tickets/TKT-1"),
                new Route(HttpMethod.GET, "/api/tickets/by-user?email=a@mail.com"),
                new Route(HttpMethod.PATCH, "/api/tickets/TKT-1/cancel"),
                new Route(HttpMethod.GET, "/api/users/by-email?email=a@mail.com"),
                new Route(HttpMethod.GET, "/api/users/by-username?username=andrea"),
                new Route(HttpMethod.GET, "/api/auth/me"));
    }

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

    private int statusOf(MockHttpServletRequestBuilder b) throws Exception {
        return mockMvc.perform(b).andReturn().getResponse().getStatus();
    }

    // ---- 401 anónimo ----
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
    @ParameterizedTest(name = "USER → 403: {0}")
    @MethodSource({"adminOnly", "staffOrAdmin"})
    void userIsForbidden(Route r) throws Exception {
        mockMvc.perform(build(r).with(user("andrea").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @ParameterizedTest(name = "STAFF → 403 en rutas ADMIN: {0}")
    @MethodSource("adminOnly")
    void staffIsForbiddenOnAdminOnly(Route r) throws Exception {
        mockMvc.perform(build(r).with(user("laura").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "ADMIN permitido: {0}")
    @MethodSource({"adminOnly", "staffOrAdmin"})
    void adminIsNotRejectedBySecurity(Route r) throws Exception {
        assertThat(statusOf(build(r).with(user("admin").roles("ADMIN")))).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "STAFF permitido: {0}")
    @MethodSource("staffOrAdmin")
    void staffIsNotRejectedBySecurity(Route r) throws Exception {
        assertThat(statusOf(build(r).with(user("laura").roles("STAFF")))).isNotIn(401, 403);
    }

    // ---- públicas ----
    @ParameterizedTest(name = "anónimo permitido: {0}")
    @MethodSource("publicRoutes")
    void publicRoutesAreOpen(Route r) throws Exception {
        assertThat(statusOf(build(r))).isNotIn(401, 403);   // 400 por body vacío en POST es válido aquí
    }

    @org.junit.jupiter.api.Test
    void purchaseRequiresUserRole() throws Exception {
        Route purchase = new Route(HttpMethod.POST, "/api/tickets");

        mockMvc.perform(build(purchase)).andExpect(status().isUnauthorized());
        mockMvc.perform(build(purchase).with(user("laura").roles("STAFF"))).andExpect(status().isForbidden());
        assertThat(statusOf(build(purchase).with(user("andrea").roles("USER")))).isNotIn(401, 403);
    }
}