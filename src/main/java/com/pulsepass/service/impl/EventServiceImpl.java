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

/**
 * Implementación de la lógica de negocio de eventos ({@link EventService}).
 *
 * <p>Reglas BR-EVENT-001 a 011: un evento nace como DRAFT, se publica solo si su fecha es futura y
 * su recinto está activo, y admite artistas mientras no esté CANCELLED ni FINISHED.</p>
 *
 * <p>Además aplica <b>visibilidad por rol</b>: el público solo ve los estados de
 * {@code PUBLIC_STATUSES}; STAFF y ADMIN ven todos (se consulta a {@link AuthorizationService}).</p>
 *
 * <p>{@code @Transactional(readOnly = true)} es el valor por defecto de la clase; los métodos que
 * escriben (create, publish, addArtist) lo sobrescriben con {@code @Transactional}.</p>
 */
@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    // NUEVO: estados visibles para el público. Ampliar aquí si el negocio lo decide.
    // Hoy solo PUBLISHED: un evento DRAFT, SOLD_OUT, CANCELLED o FINISHED únicamente lo ven STAFF/ADMIN.
    // (EnumSet es una implementación de Set optimizada para tipos enum.)
    private static final Set<EventStatus> PUBLIC_STATUSES = EnumSet.of(EventStatus.PUBLISHED);

    // Dependencias inmutables (final), inyectadas por Spring a través del constructor.
    private final EventRepository eventRepository;       // acceso a la tabla events
    private final VenueRepository venueRepository;       // buscar el recinto del evento
    private final ArtistRepository artistRepository;     // buscar artistas a asignar
    private final EventMapper eventMapper;               // entidad Event -> DTO de respuesta
    private final AuthorizationService authorizationService;          // NUEVO (¿el usuario actual es STAFF/ADMIN?)

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

    /**
     * Crea un evento en estado DRAFT (reglas BR-EVENT-001 a 006).
     *
     * @throws DuplicateResourceException si ya existe un evento con ese código (409)
     * @throws ResourceNotFoundException si el recinto (venue) no existe (404)
     * @throws BusinessRuleException si el recinto está inactivo, la fecha no es futura o la edad mínima es negativa (409)
     */
    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        // BR-EVENT-001: el eventCode debe ser único.
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event code already exists: " + request.eventCode());
        }
        // BR-EVENT-002: el recinto debe existir (se identifica por venueCode).
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));
        // BR-EVENT-003: el recinto debe estar activo.
        if (!venue.isActive()) {
            throw new BusinessRuleException("Venue is inactive: " + request.venueCode());
        }
        // BR-EVENT-004: la fecha del evento debe ser futura (ver isFuture: estrictamente posterior a hoy).
        if (!isFuture(request.eventDate())) {
            throw new BusinessRuleException("Event date must be in the future: " + request.eventDate());
        }
        // BR-EVENT-006: la edad mínima no puede ser negativa (null se interpreta como "sin restriccion" -> 0).
        int minimumAge = request.minimumAge() == null ? 0 : request.minimumAge();
        if (minimumAge < 0) {
            throw new BusinessRuleException("Minimum age cannot be negative: " + minimumAge);
        }

        // BR-EVENT-005: estado inicial siempre DRAFT, el request no lo controla
        // (CreateEventRequest ni siquiera tiene un campo "status": el cliente no puede elegirlo).
        Event event = new Event(
                request.eventCode(),
                request.name(),
                request.description(),
                request.eventDate(),
                request.category(),
                EventStatus.DRAFT,
                minimumAge);
        // Asocia el recinto (relación @ManyToOne: muchos eventos pueden realizarse en un mismo Venue).
        event.setVenue(venue);

        // Guarda el evento y devuelve un DTO (EventResponse), nunca la entidad JPA.
        return eventMapper.toResponse(eventRepository.save(event));
    }

    // CAMBIO (FR-SECEVT-02)
    /**
     * Busca un evento por código. Si el evento no está en un estado público ({@code PUBLIC_STATUSES})
     * y quien consulta no es STAFF/ADMIN, responde 404, igual que si no existiera: así un borrador
     * no revela su existencia.
     */
    @Override
    public EventResponse findByCode(String eventCode) {
        Event event = getEventOrThrow(eventCode);
        // Un borrador no revela su existencia a anónimos/USER: 404, igual que si no existiera
        if (!PUBLIC_STATUSES.contains(event.getStatus()) && !authorizationService.isStaffOrAdmin()) {
            throw new ResourceNotFoundException("Event not found: " + eventCode);
        }
        return eventMapper.toResponse(event);
    }

    /** Eventos PUBLISHED por fecha ascendente (el repositorio ya los ordena), en versión resumen. */
    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    /**
     * Publica un evento (DRAFT -> PUBLISHED). Reglas BR-EVENT-007 a 009.
     * Las condiciones se revalidan al publicar porque pudieron cambiar desde que se creó el borrador.
     */
    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = getEventOrThrow(eventCode);

        // BR-EVENT-007: solo un evento en DRAFT puede publicarse.
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Current status: " + event.getStatus());
        }
        // BR-EVENT-008: la fecha debe seguir siendo futura al momento de publicar.
        if (!isFuture(event.getEventDate())) {
            throw new BusinessRuleException("Event date must be in the future: " + eventCode);
        }
        // BR-EVENT-009: el recinto debe seguir activo al momento de publicar.
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException("Venue is inactive for event: " + eventCode);
        }

        // DRAFT -> PUBLISHED: a partir de aquí el evento es visible para el público y admite compras.
        event.changeStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    /**
     * Asigna un artista a un evento (relación muchos a muchos Event-Artist). Reglas BR-EVENT-010 y 011.
     *
     * @throws ResourceNotFoundException si el evento o el artista no existen (404)
     * @throws BusinessRuleException si el evento está CANCELLED/FINISHED o el artista ya estaba asignado (409)
     */
    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = getEventOrThrow(eventCode);

        // BR-EVENT-011: no se agregan artistas a un evento CANCELLED o FINISHED.
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Cannot add artists to an event in status " + event.getStatus());
        }

        // El artista se busca por su id; si no existe, 404.
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));

        // BR-EVENT-010: un artista no puede asignarse dos veces al mismo evento (se compara por id).
        boolean alreadyAssigned = event.getArtists().stream()
                .anyMatch(a -> artistId.equals(a.getId()));
        if (alreadyAssigned) {
            throw new BusinessRuleException(
                    "Artist " + artistId + " is already assigned to event " + eventCode);
        }

        // Agrega el artista a la colección del evento; al guardar, JPA inserta la fila en la tabla intermedia.
        event.addArtist(artist);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    // CAMBIO (FR-SECEVT-06)
    /**
     * Eventos en los que participa un artista (por nombre artístico). La consulta trae todos los
     * eventos del artista y luego se filtra en memoria: el público solo ve los de estado público;
     * STAFF/ADMIN ven todos.
     */
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

    /** Busca un evento por código o lanza ResourceNotFoundException (404). Evita repetir el mismo orElseThrow. */
    private Event getEventOrThrow(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    /**
     * ¿La fecha es estrictamente posterior a hoy? Un evento de HOY NO se considera futuro, por lo que
     * no se puede crear ni publicar (en cambio, comprar tickets de un evento de hoy sí está permitido:
     * BR-TICKET-005 usa isBefore). Una fecha nula también devuelve false.
     */
    private boolean isFuture(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }
}