package com.pulsepass.service;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface EventService {

    @PreAuthorize("hasRole('ADMIN')")
    EventResponse create(CreateEventRequest request);

    // Público; EventServiceImpl oculta los no publicados a quien no sea STAFF/ADMIN
    EventResponse findByCode(String eventCode);

    List<EventSummaryResponse> findPublishedEvents();

    @PreAuthorize("hasRole('ADMIN')")
    EventResponse publish(String eventCode);

    @PreAuthorize("hasRole('ADMIN')")
    EventResponse addArtist(String eventCode, Long artistId);

    // Público; filtrado por estado en la implementación
    List<EventSummaryResponse> findByArtist(String stageName);
}
