package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.impl.EventServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    private static final LocalDate FUTURE = LocalDate.now().plusDays(30);

    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private Venue venue;
    @Mock
    private Artist artist;

    @InjectMocks
    private EventServiceImpl eventService;

    // ---------- TEST-EVENT-001 ----------
    @Test
    void findByCode_existingEvent_returnsDto() {
        // ARRANGE
        Event event = newEvent(EventStatus.PUBLISHED, FUTURE);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(responseOf(event));

        // ACT
        EventResponse result = eventService.findByCode("CMF-2026");

        // ASSERT
        assertThat(result.eventCode()).isEqualTo("CMF-2026");
        verify(eventRepository).findByEventCode("CMF-2026");
    }

    // ---------- TEST-EVENT-002 ----------
    @Test
    void findByCode_unknownEvent_throwsResourceNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode("NOPE")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.findByCode("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NOPE");
    }

    // ---------- TEST-EVENT-003 ----------
    @Test
    void create_validRequest_savesEventAsDraft() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venue.isActive()).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class)))
                .thenAnswer(inv -> responseOf(inv.getArgument(0)));

        // ACT
        EventResponse result = eventService.create(validRequest(FUTURE));

        // ASSERT
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
    }

    // ---------- BR-EVENT-001 ----------
    @Test
    void create_duplicatedCode_throwsDuplicateResource() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest(FUTURE)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- TEST-EVENT-004 ----------
    @Test
    void create_unknownVenue_throwsAndNeverSaves() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest(FUTURE)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- TEST-EVENT-005 ----------
    @Test
    void create_inactiveVenue_throwsBusinessRule() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venue.isActive()).thenReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest(FUTURE)))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- TEST-EVENT-006 ----------
    @Test
    void create_pastDate_throwsBusinessRule() {
        // ARRANGE
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venue.isActive()).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(validRequest(LocalDate.now().minusDays(1))))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- TEST-EVENT-007 ----------
    @Test
    void publish_validDraft_becomesPublished() {
        // ARRANGE
        Event event = newEvent(EventStatus.DRAFT, FUTURE);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(venue.isActive()).thenReturn(true);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenAnswer(inv -> responseOf(event));

        // ACT
        EventResponse result = eventService.publish("CMF-2026");

        // ASSERT
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(event);
    }

    // ---------- TEST-EVENT-008 ----------
    @Test
    void publish_cancelledEvent_throwsAndNeverSaves() {
        // ARRANGE
        Event event = newEvent(EventStatus.CANCELLED, FUTURE);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
    }

    // ---------- FR-SVC-007: addArtist (unit tests requeridos) ----------
    @Test
    void addArtist_validArtist_associatesAndSaves() {
        // ARRANGE
        Event event = newEvent(EventStatus.PUBLISHED, FUTURE);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(5L)).thenReturn(Optional.of(artist));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenAnswer(inv -> responseOf(event));

        // ACT
        eventService.addArtist("CMF-2026", 5L);

        // ASSERT
        assertThat(event.getArtists()).contains(artist);
        verify(eventRepository).save(event);
    }

    @Test
    void addArtist_alreadyAssigned_throwsBusinessRule() {
        // ARRANGE
        Event event = newEvent(EventStatus.PUBLISHED, FUTURE);
        event.addArtist(artist);
        when(artist.getId()).thenReturn(5L);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(5L)).thenReturn(Optional.of(artist));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 5L))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void addArtist_cancelledEvent_throwsBusinessRule() {
        // ARRANGE
        Event event = newEvent(EventStatus.CANCELLED, FUTURE);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 5L))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- helpers ----------
    private Event newEvent(EventStatus status, LocalDate date) {
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "desc",
                date, EventCategory.MUSIC, status, 18);
        event.setVenue(venue);
        return event;
    }

    private CreateEventRequest validRequest(LocalDate date) {
        return new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, date, 18, "VEN-SMR-01");
    }

    private EventResponse responseOf(Event e) {
        return new EventResponse(1L, e.getEventCode(), e.getName(), e.getDescription(),
                e.getCategory(), e.getStatus(), e.getEventDate(), e.getMinimumAge(),
                "VEN-SMR-01", "Marina Convention Center", List.of());
    }
}
