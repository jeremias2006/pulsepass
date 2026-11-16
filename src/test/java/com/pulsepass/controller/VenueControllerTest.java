package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.security.SecurityErrorWriter;
import com.pulsepass.service.AuthorizationService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Las rutas de recintos son públicas: las pruebas se hacen sin usuario autenticado.
 */
@WebMvcTest(VenueController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SecurityErrorWriter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    // SecurityConfig los necesita para construir la cadena de filtros
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private AuthorizationService authorizationService;

    // Recinto de ejemplo (capacidad 3) que devuelve el servicio simulado.
    private VenueResponse venue() {
        return new VenueResponse(
                1L,
                "VEN-SMR-01",
                "Marina Convention Center",
                "Santa Marta",
                3,
                true
        );
    }

    // TEST-CTRL-VEN-001 / AC-CTRL001: venue existente -> 200
    // Recinto existente: 200 y todos los campos del JSON (id, código, nombre, ciudad, capacidad y estado).
    @Test
    void shouldReturnVenueByCode() throws Exception {
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(venue());

        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"))
                .andExpect(jsonPath("$.city").value("Santa Marta"))
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.active").value(true));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    // TEST-CTRL-VEN-002 / AC-CTRL002: venue inexistente -> 404 con ErrorResponse
    // Recinto inexistente: 404 con el contrato completo de error (timestamp, status, error, message y details como mapa).
    @Test
    void shouldReturn404WhenVenueDoesNotExist() throws Exception {
        when(venueService.findByCode("NO-EXISTE"))
                .thenThrow(new ResourceNotFoundException("Venue not found: NO-EXISTE"));

        mockMvc.perform(get("/api/venues/{code}", "NO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: NO-EXISTE"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // TEST-CTRL-VEN-003: venues activos -> 200
    // Lista de activos (200). Verifica que "/active" no se confundió con la ruta /{code}.
    @Test
    void shouldReturnActiveVenues() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of(venue()));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"));

        verify(venueService).findActiveVenues();
        // "active" no debe interpretarse como {code}
        verify(venueService, never()).findByCode(any());
    }

    // Error inesperado -> 500 sin exponer detalles internos (NFR-CTRL-007)
    // Un error inesperado (aquí un RuntimeException con un texto sensible) responde 500 con un mensaje genérico y
    // NO filtra el detalle interno al cliente (NFR-CTRL-007). not(containsString(...)) comprueba que el texto no aparece.
    @Test
    void shouldReturn500WithoutLeakingInternalDetails() throws Exception {
        when(venueService.findByCode("VEN-SMR-01"))
                .thenThrow(new RuntimeException("jdbc password leaked"));

        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("jdbc password leaked"))));
    }
}