package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SecurityErrorWriter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private AuthorizationService authorizationService;

    private ArtistResponse solarBeat() {
        return new ArtistResponse(1L, "Solar Beat", true);
    }

    // TEST-CTRL-ART-001: buscar por ID -> 200
    @Test
    void shouldReturnArtistById() throws Exception {
        when(artistService.findById(1L)).thenReturn(solarBeat());

        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService).findById(1L);
    }

    // TEST-CTRL-ART-002: ID inexistente -> 404
    @Test
    void shouldReturn404WhenArtistIdDoesNotExist() throws Exception {
        when(artistService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(get("/api/artists/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    // Extra: id no numérico -> 400, Service nunca invocado
    @Test
    void shouldReturn400WhenArtistIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/artists/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.id").exists());

        verify(artistService, never()).findById(any());
    }

    // TEST-CTRL-ART-003: buscar por stageName -> 200
    @Test
    void shouldReturnArtistByStageName() throws Exception {
        when(artistService.findByStageName("Solar Beat")).thenReturn(solarBeat());

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    // FR-CTRLART-002: stageName inexistente -> 404
    @Test
    void shouldReturn404WhenStageNameDoesNotExist() throws Exception {
        when(artistService.findByStageName("Nadie"))
                .thenThrow(new ResourceNotFoundException("Artist not found: Nadie"));

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Nadie"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Artist not found: Nadie"));
    }

    // TEST-CTRL-ART-004: activos -> 200
    @Test
    void shouldReturnActiveArtists() throws Exception {
        when(artistService.findActiveArtists()).thenReturn(List.of(solarBeat()));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"));

        verify(artistService).findActiveArtists();
        verify(artistService, never()).findById(any());
    }
}