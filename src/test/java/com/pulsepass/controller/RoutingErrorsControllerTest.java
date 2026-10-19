package com.pulsepass.controller;

import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.security.SecurityErrorWriter;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Errores de enrutamiento: deben salir con el contrato ErrorResponse y el código correcto,
 * nunca como 500. Con los filtros de seguridad reales activos (no se usa addFilters=false).
 */
@WebMvcTest({VenueController.class, AuthController.class})
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SecurityErrorWriter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class RoutingErrorsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;
    @MockitoBean
    private AuthService authService;

    // SecurityConfig los necesita para construir la cadena de filtros
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    @WithMockUser(roles = "USER")
    void unknownRouteReturns404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/api/ruta-que-no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    @WithMockUser(roles = "USER")
    void unsupportedVerbReturns405WithAllowHeader() throws Exception {
        // /api/venues/active solo admite GET
        mockMvc.perform(post("/api/venues/active"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void wrongContentTypeOnLoginReturns415WithErrorResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("identifier=andrea"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    void anonymousOnUnknownRouteStillGets401NotInfoAboutRoutes() throws Exception {
        // anyRequest().authenticated(): un anónimo no puede distinguir rutas existentes de inexistentes
        mockMvc.perform(get("/api/ruta-que-no-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
