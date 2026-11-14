package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST de eventos: {@code /api/events}. Las consultas son públicas; crear, publicar y
 * asignar artistas son exclusivos de ADMIN.
 *
 * <p>Capa Controller: solo traduce entre HTTP y Java. Lee la petición (ruta, parámetros, cuerpo JSON),
 * delega TODO el trabajo en el servicio y devuelve un {@code ResponseEntity} con el código HTTP y el
 * cuerpo. No contiene reglas de negocio ni habla con los repositorios.</p>
 *
 * <p>Anotaciones habituales: {@code @RestController} (cada método devuelve datos que Spring serializa
 * a JSON), {@code @RequestMapping} (prefijo común de las rutas) y {@code @GetMapping} /
 * {@code @PostMapping} / {@code @PatchMapping} (verbo HTTP + ruta). Los errores no se manejan aquí:
 * las excepciones las traduce {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;   // inyectado por constructor

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // POST /api/events  ->  201 Created
    // Solo ADMIN. Crea el evento en estado DRAFT. @Valid valida la FORMA del JSON (campos obligatorios,
    // longitudes); las reglas de NEGOCIO (fecha futura, recinto activo...) las valida el servicio.
    // 201 en lugar de 200 porque se crea un recurso nuevo.
    @PostMapping
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody CreateEventRequest request) {

        EventResponse response = eventService.create(request);

        // ResponseEntity permite elegir el código HTTP (201) y el cuerpo (el evento creado).
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET /api/events/published
    // Público. Lista resumida de los eventos PUBLISHED.
    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {

        return ResponseEntity.ok(eventService.findPublishedEvents());
    }

    // GET /api/events/by-artist?stageName=Solar Beat
    // Público. Eventos en los que participa el artista; el servicio oculta los no publicados a quien
    // no sea STAFF/ADMIN.
    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(
            @RequestParam String stageName) {

        return ResponseEntity.ok(eventService.findByArtist(stageName));
    }

    // GET /api/events/{eventCode}
    // Público. Un evento no publicado solo lo ve STAFF/ADMIN; para los demás responde 404.
    // Las rutas literales (/published, /by-artist) tienen prioridad sobre esta ruta con variable.
    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(
            @PathVariable String eventCode) {

        return ResponseEntity.ok(eventService.findByCode(eventCode));
    }

    // PATCH /api/events/{eventCode}/publish
    // Solo ADMIN. PATCH = modificación parcial: aquí solo cambia el estado (DRAFT -> PUBLISHED).
    @PatchMapping("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(
            @PathVariable String eventCode) {

        return ResponseEntity.ok(eventService.publish(eventCode));
    }

    // POST /api/events/{eventCode}/artists/{artistId}
    // Solo ADMIN. Asigna un artista existente al evento y devuelve el evento actualizado (200).
    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(
            @PathVariable String eventCode,
            @PathVariable Long artistId) {

        return ResponseEntity.ok(eventService.addArtist(eventCode, artistId));
    }
}
