package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.AuthorizationService;      // NUEVO
import com.pulsepass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;                               // NUEVO
import java.util.List;
import java.util.Set;                                   // NUEVO

@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    // NUEVO: estados visibles para el público. Ampliar aquí si el negocio lo decide.
    private static final Set<EventStatus> PUBLIC_STATUSES = EnumSet.of(EventStatus.PUBLISHED);

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;
    private final AuthorizationService authorizationService;          // NUEVO

    public EventServiceImpl(EventRepository eventRepository,
                            VenueRepository venueRepository,
                            ArtistRepository artistRepository,
                            EventMapper eventMapper,
                            AuthorizationService authorizationService) {   // NUEVO (último parámetro)
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        // BR-EVENT-001
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event code already exists: " + request.eventCode());
        }
        // BR-EVENT-002
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));
        // BR-EVENT-003
        if (!venue.isActive()) {
            throw new BusinessRuleException("Venue is inactive: " + request.venueCode());
        }
        // BR-EVENT-004
        if (!isFuture(request.eventDate())) {
            throw new BusinessRuleException("Event date must be in the future: " + request.eventDate());
        }
        // BR-EVENT-006 (null se interpreta como "sin restriccion")
        int minimumAge = request.minimumAge() == null ? 0 : request.minimumAge();
        if (minimumAge < 0) {
            throw new BusinessRuleException("Minimum age cannot be negative: " + minimumAge);
        }

        // BR-EVENT-005: estado inicial siempre DRAFT, el request no lo controla
        Event event = new Event(
                request.eventCode(),
                request.name(),
                request.description(),
                request.eventDate(),
                request.category(),
                EventStatus.DRAFT,
                minimumAge);
        event.setVenue(venue);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    // CAMBIO (FR-SECEVT-02)
    @Override
    public EventResponse findByCode(String eventCode) {
        Event event = getEventOrThrow(eventCode);
        // Un borrador no revela su existencia a anónimos/USER: 404, igual que si no existiera
        if (!PUBLIC_STATUSES.contains(event.getStatus()) && !authorizationService.isStaffOrAdmin()) {
            throw new ResourceNotFoundException("Event not found: " + eventCode);
        }
        return eventMapper.toResponse(event);
    }

    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = getEventOrThrow(eventCode);

        // BR-EVENT-007
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Current status: " + event.getStatus());
        }
        // BR-EVENT-008
        if (!isFuture(event.getEventDate())) {
            throw new BusinessRuleException("Event date must be in the future: " + eventCode);
        }
        // BR-EVENT-009
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException("Venue is inactive for event: " + eventCode);
        }

        event.changeStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = getEventOrThrow(eventCode);

        // BR-EVENT-011
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Cannot add artists to an event in status " + event.getStatus());
        }

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));

        // BR-EVENT-010
        boolean alreadyAssigned = event.getArtists().stream()
                .anyMatch(a -> artistId.equals(a.getId()));
        if (alreadyAssigned) {
            throw new BusinessRuleException(
                    "Artist " + artistId + " is already assigned to event " + eventCode);
        }

        event.addArtist(artist);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    // CAMBIO (FR-SECEVT-06)
    @Override
    public List<EventSummaryResponse> findByArtist(String stageName) {
        // El público solo ve eventos publicados; STAFF/ADMIN ven todos
        boolean privileged = authorizationService.isStaffOrAdmin();
        return eventRepository.findByArtistStageName(stageName)
                .stream()
                .filter(e -> privileged || PUBLIC_STATUSES.contains(e.getStatus()))
                .map(eventMapper::toSummary)
                .toList();
    }

    private Event getEventOrThrow(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    private boolean isFuture(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }
}