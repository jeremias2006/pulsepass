package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final LocalDate EVENT_DATE = LocalDate.now().plusDays(30);
    private static final String EMAIL = "andrea@email.com";

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketPriceCalculator priceCalculator;
    @Mock
    private Venue venue;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private PurchaseTicketRequest request() {
        return new PurchaseTicketRequest(EMAIL, "CMF-2026", TicketType.VIP);
    }

    // ---------- TEST-TICKET-001 ----------
    @Test
    void purchase_validRequest_createsPaidTicket() {
        // ARRANGE
        User andrea = userWithAge(25);
        Event event = newEvent(EventStatus.PUBLISHED, EVENT_DATE, 18);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(andrea));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(venue.getCapacity()).thenReturn(3);
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID))
                .thenReturn(0L);
        when(priceCalculator.calculate(TicketType.VIP)).thenReturn(new BigDecimal("100.00"));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenAnswer(inv -> responseOf(inv.getArgument(0)));

        // ACT
        TicketResponse result = ticketService.purchase(request());

        // ASSERT
        assertThat(result.status()).isEqualTo(TicketStatus.PAID);
        assertThat(result.price()).isEqualByComparingTo("100.00");
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- TEST-TICKET-002 ----------
    @Test
    void purchase_unknownUser_throwsResourceNotFound() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- TEST-TICKET-003 ----------
    @Test
    void purchase_inactiveUser_throwsBusinessRule() {
        // ARRANGE
        User miguel = new User("miguel", EMAIL);
        miguel.deactivate();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(miguel));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- TEST-TICKET-004 ----------
    @Test
    void purchase_draftEvent_throwsBusinessRule() {
        // ARRANGE
        stubUserAndEvent(new User("andrea", EMAIL), newEvent(EventStatus.DRAFT, EVENT_DATE, 0));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- TEST-TICKET-005 ----------
    @Test
    void purchase_cancelledEvent_throwsBusinessRule() {
        // ARRANGE
        stubUserAndEvent(new User("andrea", EMAIL), newEvent(EventStatus.CANCELLED, EVENT_DATE, 0));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- TEST-TICKET-006 ----------
    @Test
    void purchase_underageUser_throwsBusinessRule() {
        // ARRANGE
        stubUserAndEvent(userWithAge(17), newEvent(EventStatus.PUBLISHED, EVENT_DATE, 18));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minimum age");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- TEST-TICKET-007 ----------
    @Test
    void purchase_noCapacityLeft_throwsBusinessRule() {
        // ARRANGE
        stubUserAndEvent(new User("andrea", EMAIL), newEvent(EventStatus.PUBLISHED, EVENT_DATE, 0));
        when(venue.getCapacity()).thenReturn(3);
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID))
                .thenReturn(3L);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(request()))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- TEST-TICKET-008 ----------
    @Test
    void purchase_lastAvailableTicket_marksEventSoldOut() {
        // ARRANGE
        Event event = newEvent(EventStatus.PUBLISHED, EVENT_DATE, 0);
        stubUserAndEvent(new User("andrea", EMAIL), event);
        when(venue.getCapacity()).thenReturn(3);
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID))
                .thenReturn(2L);
        when(priceCalculator.calculate(TicketType.VIP)).thenReturn(new BigDecimal("100.00"));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.save(event)).thenReturn(event);
        when(ticketMapper.toResponse(any(Ticket.class)))
                .thenAnswer(inv -> responseOf(inv.getArgument(0)));

        // ACT
        ticketService.purchase(request());

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
        verify(eventRepository).save(event);
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
    }

    // ---------- TEST-TICKET-009 ----------
    @Test
    void cancel_paidTicket_becomesCancelled() {
        // ARRANGE
        Ticket ticket = newTicket(TicketStatus.PAID, newEvent(EventStatus.PUBLISHED, EVENT_DATE, 0));
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenAnswer(inv -> responseOf(ticket));

        // ACT
        TicketResponse result = ticketService.cancel("TKT-0001");

        // ASSERT
        assertThat(result.status()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(ticket);
    }

    // ---------- TEST-TICKET-010 ----------
    @Test
    void cancel_usedTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = newTicket(TicketStatus.USED, newEvent(EventStatus.PUBLISHED, EVENT_DATE, 0));
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.cancel("TKT-0001"))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- BR-TICKET-012 ----------
    @Test
    void cancel_afterEventDate_throwsBusinessRule() {
        // ARRANGE
        Event pastEvent = newEvent(EventStatus.PUBLISHED, LocalDate.now().minusDays(1), 0);
        Ticket ticket = newTicket(TicketStatus.PAID, pastEvent);
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.cancel("TKT-0001"))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- TEST-TICKET-011 ----------
    @Test
    void markAsUsed_paidTicket_becomesUsed() {
        // ARRANGE
        Ticket ticket = newTicket(TicketStatus.PAID, newEvent(EventStatus.PUBLISHED, EVENT_DATE, 0));
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenAnswer(inv -> responseOf(ticket));

        // ACT
        TicketResponse result = ticketService.markAsUsed("TKT-0001");

        // ASSERT
        assertThat(result.status()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository).save(eq(ticket));
    }

    // ---------- TEST-TICKET-012 ----------
    @Test
    void markAsUsed_cancelledTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = newTicket(TicketStatus.CANCELLED, newEvent(EventStatus.PUBLISHED, EVENT_DATE, 0));
        when(ticketRepository.findByTicketCode("TKT-0001")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.markAsUsed("TKT-0001"))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- helpers ----------
    private void stubUserAndEvent(User user, Event event) {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
    }

    /** Usuario cuya edad EN LA FECHA DEL EVENTO es exactamente {@code age}. */
    private User userWithAge(int age) {
        User user = new User("andrea", EMAIL);
        UserProfile profile = mock(UserProfile.class);
        when(profile.getBirthDate()).thenReturn(EVENT_DATE.minusYears(age));
        user.assignProfile(profile);
        return user;
    }

    private Event newEvent(EventStatus status, LocalDate date, int minimumAge) {
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "desc",
                date, EventCategory.MUSIC, status, minimumAge);
        event.setVenue(venue);
        return event;
    }

    private Ticket newTicket(TicketStatus status, Event event) {
        return new Ticket("TKT-0001", TicketType.GENERAL, new BigDecimal("50.00"), status,
                LocalDateTime.now(), new User("andrea", EMAIL), event);
    }

    private TicketResponse responseOf(Ticket t) {
        return new TicketResponse(1L, t.getTicketCode(), t.getType(), t.getPrice(),
                t.getStatus(), t.getPurchaseDate(), t.getUser().getEmail(),
                t.getEvent().getEventCode(), t.getEvent().getName());
    }
}