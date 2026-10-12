package com.pulsepass.controller;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private static final String VALID_BODY = """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "description": "Festival de musica del Caribe",
              "category": "MUSIC",
              "eventDate": "2026-12-20",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    private EventResponse event(EventStatus eventStatus, List<ArtistResponse> artists) {
        return new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival de musica del Caribe",
                EventCategory.MUSIC,
                eventStatus,
                LocalDate.of(2026, 12, 20),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                artists
        );
    }

    private EventSummaryResponse summary() {
        return new EventSummaryResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDate.of(2026, 12, 20),
                "Marina Convention Center"
        );
    }

    // TEST-CTRL-EVT-001 / AC-CTRL003: POST válido -> 201
    @Test
    void shouldCreateEvent() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenReturn(event(EventStatus.DRAFT, List.of()));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.category").value("MUSIC"))
                .andExpect(jsonPath("$.eventDate").value("2026-12-20"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    // TEST-CTRL-EVT-002 / AC-CTRL004: POST inválido -> 400 + details, Service nunca invocado
    @Test
    void shouldReturn400WhenEventRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventCode": "",
                                  "name": "Caribbean Music Fest 2026",
                                  "category": "MUSIC",
                                  "eventDate": "2026-12-20",
                                  "minimumAge": 18,
                                  "venueCode": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(eventService, never()).create(any());
    }

    // Extra: campos obligatorios ausentes (category, eventDate, minimumAge)
    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventCode": "CMF-2026",
                                  "name": "Caribbean Music Fest 2026",
                                  "venueCode": "VEN-SMR-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.category").value("Category is required"))
                .andExpect(jsonPath("$.details.eventDate").value("Event date is required"))
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age is required"));

        verify(eventService, never()).create(any());
    }

    // Extra: minimumAge negativo -> 400 (valor mínimo es validación estructural)
    @Test
    void shouldReturn400WhenMinimumAgeIsNegative() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"minimumAge\": 18", "\"minimumAge\": -1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.minimumAge")
                        .value("Minimum age must be zero or greater"));

        verify(eventService, never()).create(any());
    }

    // Extra: categoría fuera del enum -> 400 (JSON inválido)
    @Test
    void shouldReturn400WhenCategoryIsNotInEnum() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("MUSIC", "NOT_A_CATEGORY")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").exists());

        verify(eventService, never()).create(any());
    }

    // Extra: JSON mal formado -> 400
    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(eventService, never()).create(any());
    }

    // Extra: eventCode duplicado (DuplicateResourceException) -> 409
    @Test
    void shouldReturn409WhenEventCodeAlreadyExists() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new DuplicateResourceException("Event code already exists: CMF-2026"));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Event code already exists: CMF-2026"));
    }

    // Extra: venue inexistente al crear -> 404
    @Test
    void shouldReturn404WhenVenueDoesNotExistOnCreate() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-SMR-01"));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-SMR-01"));
    }

    // Extra: venue inactivo / fecha pasada (BusinessRuleException) -> 409
    @Test
    void shouldReturn409WhenVenueIsInactiveOnCreate() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenThrow(new BusinessRuleException("Venue is inactive: VEN-SMR-01"));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Venue is inactive: VEN-SMR-01"));
    }

    // TEST-CTRL-EVT-003: GET existente -> 200
    @Test
    void shouldReturnEventByCode() throws Exception {
        when(eventService.findByCode("CMF-2026"))
                .thenReturn(event(EventStatus.DRAFT, List.of()));

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.venueName").value("Marina Convention Center"));

        verify(eventService).findByCode("CMF-2026");
    }

    // TEST-CTRL-EVT-004: GET inexistente -> 404
    @Test
    void shouldReturn404WhenEventDoesNotExist() throws Exception {
        when(eventService.findByCode("CMF-2026"))
                .thenThrow(new ResourceNotFoundException("Event not found: CMF-2026"));

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event not found: CMF-2026"));
    }

    // TEST-CTRL-EVT-005: publicados -> 200
    @Test
    void shouldReturnPublishedEvents() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
        // "published" no debe interpretarse como {eventCode}
        verify(eventService, never()).findByCode(any());
    }

    // TEST-CTRL-EVT-006 / AC-CTRL005: publicar válido -> 200
    @Test
    void shouldPublishEvent() throws Exception {
        when(eventService.publish("CMF-2026"))
                .thenReturn(event(EventStatus.PUBLISHED, List.of()));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-007 / AC-CTRL006: publicar inválido -> 409
    @Test
    void shouldReturn409WhenEventCannotBePublished() throws Exception {
        when(eventService.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException(
                        "Only DRAFT events can be published. Current status: PUBLISHED"));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Only DRAFT events can be published. Current status: PUBLISHED"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // TEST-CTRL-EVT-008: asociar artista -> 200
    @Test
    void shouldAddArtistToEvent() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L))
                .thenReturn(event(EventStatus.DRAFT,
                        List.of(new ArtistResponse(1L, "Solar Beat", true))));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    // Extra: artista ya asociado / evento CANCELLED o FINISHED -> 409
    @Test
    void shouldReturn409WhenArtistIsAlreadyAssigned() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L))
                .thenThrow(new BusinessRuleException(
                        "Artist 1 is already assigned to event CMF-2026"));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Artist 1 is already assigned to event CMF-2026"));
    }

    // Extra: artista inexistente -> 404
    @Test
    void shouldReturn404WhenArtistDoesNotExist() throws Exception {
        when(eventService.addArtist("CMF-2026", 99L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    // Extra: artistId no numérico -> 400, Service nunca invocado
    @Test
    void shouldReturn400WhenArtistIdIsNotNumeric() throws Exception {
        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.artistId").exists());

        verify(eventService, never()).addArtist(anyString(), anyLong());
    }

    // TEST-CTRL-EVT-009: buscar por artista -> 200
    @Test
    void shouldReturnEventsByArtist() throws Exception {
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }

    // Extra: falta el parámetro stageName -> 400 (no 500)
    @Test
    void shouldReturn400WhenStageNameParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/events/by-artist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter"))
                .andExpect(jsonPath("$.details.stageName").exists());

        verify(eventService, never()).findByArtist(any());
    }
}
