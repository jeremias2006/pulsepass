package com.pulsepass.service;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Contrato de la lógica de negocio de eventos: crear, publicar, asignar artistas y consultar.
 * La implementación es {@code EventServiceImpl}.
 *
 * <p>Las operaciones que modifican datos (crear, publicar, agregar artista) exigen rol ADMIN
 * mediante {@code @PreAuthorize}. Las consultas son públicas a nivel de método; qué eventos ve
 * cada quien lo decide la implementación según el rol del usuario.</p>
 */
public interface EventService {

    /**
     * Crea un evento nuevo, siempre en estado DRAFT (borrador). Solo ADMIN.
     * Reglas de negocio: BR-EVENT-001 a 006.
     */
    @PreAuthorize("hasRole('ADMIN')")
    EventResponse create(CreateEventRequest request);

    /**
     * Busca un evento por su código.
     *
     * <p>Público; EventServiceImpl oculta los no publicados a quien no sea STAFF/ADMIN.</p>
     */
    EventResponse findByCode(String eventCode);

    /** Público. Lista los eventos PUBLISHED en versión resumen, ordenados por fecha ascendente. */
    List<EventSummaryResponse> findPublishedEvents();

    /**
     * Publica un evento (DRAFT -> PUBLISHED) para que el público lo vea y pueda comprar tickets.
     * Solo ADMIN. Reglas de negocio: BR-EVENT-007 a 009.
     */
    @PreAuthorize("hasRole('ADMIN')")
    EventResponse publish(String eventCode);

    /**
     * Asigna un artista existente a un evento. Solo ADMIN. Reglas de negocio: BR-EVENT-010 y 011.
     */
    @PreAuthorize("hasRole('ADMIN')")
    EventResponse addArtist(String eventCode, Long artistId);

    /**
     * Lista los eventos en los que participa un artista (por nombre artístico).
     *
     * <p>Público; filtrado por estado en la implementación.</p>
     */
    List<EventSummaryResponse> findByArtist(String stageName);
}
