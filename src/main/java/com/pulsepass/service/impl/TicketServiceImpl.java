package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketPriceCalculator;
import com.pulsepass.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;
    private final TicketPriceCalculator priceCalculator;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             UserRepository userRepository,
                             EventRepository eventRepository,
                             TicketMapper ticketMapper,
                             TicketPriceCalculator priceCalculator) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
        this.priceCalculator = priceCalculator;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // BR-TICKET-001
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));
        // BR-TICKET-002
        if (!user.isActive()) {
            throw new BusinessRuleException("Inactive user cannot purchase tickets: " + request.userEmail());
        }
        // BR-TICKET-003
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));
        // BR-TICKET-004
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events. Current status: " + event.getStatus());
        }
        // BR-TICKET-005
        if (event.getEventDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Event already took place: " + event.getEventCode());
        }
        // BR-TICKET-006
        validateMinimumAge(user, event);

        // BR-TICKET-007
        long paidTickets = ticketRepository
                .countByEventEventCodeAndStatus(event.getEventCode(), TicketStatus.PAID);
        int capacity = event.getVenue().getCapacity();
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no capacity left: " + event.getEventCode());
        }

        // BR-TICKET-009
        BigDecimal price = priceCalculator.calculate(request.type());
        if (price.signum() < 0) {
            throw new BusinessRuleException("Ticket price cannot be negative");
        }

        Ticket ticket = new Ticket(
                generateTicketCode(),
                request.type(),
                price,
                TicketStatus.PAID,
                LocalDateTime.now(),
                user,
                event);
        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: SOLD_OUT en la misma transaccion
        if (paidTickets + 1 == capacity) {
            event.changeStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    @Override
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(getTicketOrThrow(ticketCode));
    }

    @Override
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEventEventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = getTicketOrThrow(ticketCode);

        // BR-TICKET-010 / BR-TICKET-011
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Current status: " + ticket.getStatus());
        }
        // BR-TICKET-012
        if (LocalDate.now().isAfter(ticket.getEvent().getEventDate())) {
            throw new BusinessRuleException(
                    "Ticket cannot be cancelled after the event date: " + ticketCode);
        }

        ticket.changeStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = getTicketOrThrow(ticketCode);

        // BR-TICKET-014
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled ticket cannot be used: " + ticketCode);
        }
        // BR-TICKET-013
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be used. Current status: " + ticket.getStatus());
        }

        ticket.changeStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    private Ticket getTicketOrThrow(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    private void validateMinimumAge(User user, Event event) {
        Integer minimumAge = event.getMinimumAge();
        if (minimumAge == null || minimumAge <= 0) {
            return;
        }
        UserProfile profile = user.getProfile();
        if (profile == null || profile.getBirthDate() == null) {
            throw new BusinessRuleException("User has no birth date registered: " + user.getEmail());
        }
        // La edad se evalua en la fecha del evento, no hoy
        int age = Period.between(profile.getBirthDate(), event.getEventDate()).getYears();
        if (age < minimumAge) {
            throw new BusinessRuleException(
                    "User does not meet minimum age of " + minimumAge
                            + " for event " + event.getEventCode());
        }
    }

    private String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}