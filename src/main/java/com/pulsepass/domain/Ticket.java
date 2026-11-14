package com.pulsepass.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ticket (entrada) comprado por un usuario para un evento. Tabla {@code tickets} (migración V1).
 *
 * <p><b>Entidad JPA:</b> cada instancia es una fila de la tabla indicada. {@code @Id} junto con
 * {@code @GeneratedValue(IDENTITY)} significa que la clave primaria la genera la base de datos
 * (BIGSERIAL en PostgreSQL). El constructor protegido sin argumentos lo exige JPA (Hibernate lo usa
 * por reflexión); el constructor público es el que usa nuestro código. Casi no hay setters: los
 * cambios pasan por métodos con nombre propio. El esquema real lo crean las migraciones de Flyway
 * ({@code db/migration}); con {@code ddl-auto=validate} Hibernate solo comprueba al arrancar que la
 * entidad y la tabla coincidan.</p>
 *
 * <p>Relaciones: es el lado dueño de dos N:1, hacia {@link User} ({@code user_id}) y hacia {@link Event}
 * ({@code event_id}); ambas son LAZY. Índices en la base: {@code user_id}, {@code event_id} y {@code status}.</p>
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código de negocio único con formato TKT-XXXXXXXX (lo genera TicketServiceImpl). Máx. 30 caracteres.
    @Column(name = "ticket_code", nullable = false, unique = true)
    private String ticketCode;

    // Se guarda el nombre del enum ("VIP"); la base lo restringe con un CHECK (chk_ticket_type).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType type;

    // Dinero exacto: BigDecimal <-> NUMERIC(10,2) (hasta 10 dígitos en total, 2 decimales).
    // La base tiene además CHECK (price >= 0), que respalda BR-TICKET-009.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Estado del ticket (ver TicketStatus), con CHECK en la base (chk_ticket_status).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "purchase_date", nullable = false)
    private LocalDateTime purchaseDate;

    // Dueño del N:1 hacia User (clave foránea user_id, obligatoria). LAZY: el usuario no se carga hasta que se usa.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Dueño del N:1 hacia Event (clave foránea event_id, obligatoria). LAZY, igual que user.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // Constructor sin argumentos: lo exige JPA (Hibernate crea las entidades por reflexión). Es protected
    // para que el resto del código no pueda crear una entidad vacía.
    protected Ticket() {
    }

    public Ticket(String ticketCode, TicketType type, BigDecimal price, TicketStatus status,
                  LocalDateTime purchaseDate, User user, Event event) {
        this.ticketCode = ticketCode;
        this.type = type;
        this.price = price;
        this.status = status;
        this.purchaseDate = purchaseDate;
        this.user = user;
        this.event = event;
    }

    // Único punto de cambio del estado. Lo llaman cancel (-> CANCELLED) y markAsUsed (-> USED).
    public void changeStatus(TicketStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public TicketType getType() {
        return type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public User getUser() {
        return user;
    }

    public Event getEvent() {
        return event;
    }
}