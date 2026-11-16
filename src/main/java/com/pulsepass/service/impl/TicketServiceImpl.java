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

/**
 * Implementación de la lógica de negocio de tickets ({@link TicketService}).
 *
 * <p>Capa Service: aquí viven las reglas BR-TICKET-001 a 014 (qué se puede comprar, cancelar o
 * usar, y cuándo). No conoce HTTP: cuando una regla se viola lanza una excepción de dominio y
 * {@code GlobalExceptionHandler} la traduce a la respuesta HTTP
 * ({@code ResourceNotFoundException} -> 404, {@code BusinessRuleException} -> 409).</p>
 *
 * <p>Anotaciones de clase:</p>
 * <ul>
 *   <li>{@code @Service}: Spring crea y administra esta clase como bean.</li>
 *   <li>{@code @Transactional(readOnly = true)}: por defecto todos los métodos son de solo
 *       lectura; los que escriben (purchase, cancel, markAsUsed) lo sobrescriben con
 *       {@code @Transactional}.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    // Dependencias inmutables (final), inyectadas por Spring a través del constructor.
    private final TicketRepository ticketRepository;     // acceso a la tabla tickets
    private final UserRepository userRepository;         // buscar al comprador
    private final EventRepository eventRepository;       // buscar y actualizar el evento
    private final TicketMapper ticketMapper;             // entidad Ticket -> TicketResponse (DTO)
    private final TicketPriceCalculator priceCalculator; // precio según el tipo de ticket

    // Inyección por constructor: con un único constructor Spring lo usa sin necesidad de @Autowired.
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

    /**
     * Compra un ticket (reglas BR-TICKET-001 a 009).
     *
     * <p>Flujo: valida usuario -> valida evento -> valida edad y cupo -> calcula el precio en el
     * servidor -> guarda el ticket como PAID -> si era el último cupo, marca el evento SOLD_OUT.
     * Todo ocurre en UNA transacción ({@code @Transactional}): si cualquier paso falla se revierte
     * completo, y no puede quedar un ticket guardado sin haber actualizado el evento.</p>
     *
     * <p>Las validaciones siguen el orden de los códigos BR-TICKET-001 a 007: se corta en la
     * primera regla que falla.</p>
     *
     * @param request email del comprador, código del evento y tipo de ticket
     * @return el ticket creado, ya en estado PAID
     * @throws ResourceNotFoundException si el usuario o el evento no existen (404)
     * @throws BusinessRuleException si alguna regla de negocio no se cumple (409)
     */
    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // BR-TICKET-001: el comprador debe existir (búsqueda por email, sin distinguir mayúsculas). Si no, 404.
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));
        // BR-TICKET-002: el usuario debe estar activo.
        if (!user.isActive()) {
            throw new BusinessRuleException("Inactive user cannot purchase tickets: " + request.userEmail());
        }
        // BR-TICKET-003: el evento debe existir (búsqueda por eventCode). Si no, 404.
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));
        // BR-TICKET-004: solo se venden tickets de eventos PUBLISHED (no DRAFT, SOLD_OUT, CANCELLED ni FINISHED).
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events. Current status: " + event.getStatus());
        }
        // BR-TICKET-005: no se vende para eventos pasados. Con isBefore(hoy), un evento que es HOY todavía se puede comprar.
        if (event.getEventDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Event already took place: " + event.getEventCode());
        }
        // BR-TICKET-006: edad mínima exigida por el evento (detalle en validateMinimumAge).
        validateMinimumAge(user, event);

        // BR-TICKET-007: control de cupo. Se cuentan los tickets PAID del evento (los CANCELLED no ocupan lugar)
        // y se comparan con la capacidad del recinto (Venue) donde se realiza.
        // Limitación: no hay bloqueo (@Lock/@Version) entre este conteo y el INSERT, así que dos compras
        // simultáneas del último cupo podrían pasar ambas (ver pregunta 6 del README).
        long paidTickets = ticketRepository
                .countByEventEventCodeAndStatus(event.getEventCode(), TicketStatus.PAID);
        int capacity = event.getVenue().getCapacity();
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no capacity left: " + event.getEventCode());
        }

        // BR-TICKET-009: el precio lo calcula el servidor según el tipo (el cliente no lo envía) y no puede ser negativo.
        BigDecimal price = priceCalculator.calculate(request.type());
        if (price.signum() < 0) {
            throw new BusinessRuleException("Ticket price cannot be negative");
        }

        // El ticket nace directamente como PAID (este flujo no usa el estado RESERVED).
        // purchaseDate = ahora; ticketCode = código aleatorio legible (ver generateTicketCode).
        Ticket ticket = new Ticket(
                generateTicketCode(),
                request.type(),
                price,
                TicketStatus.PAID,
                LocalDateTime.now(),
                user,
                event);
        // Persiste el ticket y devuelve la entidad guardada (ya con su id generado).
        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: SOLD_OUT en la misma transaccion
        // paidTickets se leyó ANTES de guardar este ticket, por eso el "+ 1": si con este ticket se llena
        // el recinto, el evento pasa a SOLD_OUT y ya no aceptará más compras (BR-TICKET-004).
        if (paidTickets + 1 == capacity) {
            event.changeStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        // Se devuelve un DTO (TicketResponse), nunca la entidad JPA directamente.
        return ticketMapper.toResponse(saved);
    }

    /** Busca un ticket por código; si no existe lanza 404. Solo lectura (hereda readOnly de la clase). */
    @Override
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(getTicketOrThrow(ticketCode));
    }

    /** Tickets de un usuario ordenados por fecha de compra descendente (el método del repositorio ya los ordena). */
    @Override
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    /** Tickets PAID de un evento. Cada entidad se convierte a DTO con el mapper (stream -> map -> toList). */
    @Override
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEventEventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    /**
     * Cancela un ticket (PAID -> CANCELLED). Reglas BR-TICKET-010 a 012.
     * Quién puede hacerlo ya fue validado por {@code @PreAuthorize} antes de entrar aquí.
     */
    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = getTicketOrThrow(ticketCode);

        // BR-TICKET-010 / BR-TICKET-011: solo un ticket PAID puede cancelarse (excluye los ya CANCELLED, USED o RESERVED).
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Current status: " + ticket.getStatus());
        }
        // BR-TICKET-012: no se cancela después de la fecha del evento (el mismo día del evento todavía se permite).
        if (LocalDate.now().isAfter(ticket.getEvent().getEventDate())) {
            throw new BusinessRuleException(
                    "Ticket cannot be cancelled after the event date: " + ticketCode);
        }

        // Cambia el estado en la entidad y la guarda. Al cancelar, el ticket deja de contar como PAID (libera cupo).
        ticket.changeStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    /** Marca un ticket como usado (PAID -> USED). Reglas BR-TICKET-013 y 014. */
    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = getTicketOrThrow(ticketCode);

        // BR-TICKET-014: un ticket CANCELLED no se puede usar. Se revisa primero para dar un mensaje más específico
        // que el genérico de BR-TICKET-013.
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled ticket cannot be used: " + ticketCode);
        }
        // BR-TICKET-013: solo un ticket PAID puede usarse (un ticket ya USED no se puede usar dos veces).
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be used. Current status: " + ticket.getStatus());
        }

        // PAID -> USED y se persiste.
        ticket.changeStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    /** Busca un ticket por código o lanza ResourceNotFoundException (404). Evita repetir el mismo orElseThrow. */
    private Ticket getTicketOrThrow(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    /**
     * BR-TICKET-006: verifica que el usuario cumpla la edad mínima del evento.
     *
     * @throws BusinessRuleException si el evento exige edad y el usuario no tiene fecha de nacimiento
     *                               registrada, o si es menor de la edad exigida
     */
    private void validateMinimumAge(User user, Event event) {
        Integer minimumAge = event.getMinimumAge();
        // Sin restricción (null o 0): no hay nada que validar.
        if (minimumAge == null || minimumAge <= 0) {
            return;
        }
        UserProfile profile = user.getProfile();
        // El evento exige edad pero no sabemos cuándo nació el usuario: no se puede verificar, se rechaza.
        if (profile == null || profile.getBirthDate() == null) {
            throw new BusinessRuleException("User has no birth date registered: " + user.getEmail());
        }
        // La edad se evalua en la fecha del evento, no hoy
        // (Period.between(...).getYears() devuelve años COMPLETOS cumplidos a esa fecha).
        int age = Period.between(profile.getBirthDate(), event.getEventDate()).getYears();
        if (age < minimumAge) {
            throw new BusinessRuleException(
                    "User does not meet minimum age of " + minimumAge
                            + " for event " + event.getEventCode());
        }
    }

    /**
     * Genera un código legible con el formato {@code TKT-XXXXXXXX}: el prefijo "TKT-" más los
     * primeros 8 caracteres (hexadecimales, en mayúsculas) de un UUID aleatorio.
     * La unicidad la garantiza en última instancia la restricción UNIQUE de {@code ticket_code}
     * en la base (V1__create_schema.sql): una colisión es improbable, pero si ocurriera el INSERT fallaría.
     */
    private String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}