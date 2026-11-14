package com.pulsepass.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evento (concierto, partido, conferencia...). Tabla {@code events} (V1; la columna {@code streaming_url}
 * se agregó en V3). Es la entidad central del modelo.
 *
 * <p><b>Entidad JPA:</b> cada instancia es una fila de la tabla indicada. {@code @Id} junto con
 * {@code @GeneratedValue(IDENTITY)} significa que la clave primaria la genera la base de datos
 * (BIGSERIAL en PostgreSQL). El constructor protegido sin argumentos lo exige JPA (Hibernate lo usa
 * por reflexión); el constructor público es el que usa nuestro código. Casi no hay setters: los
 * cambios pasan por métodos con nombre propio. El esquema real lo crean las migraciones de Flyway
 * ({@code db/migration}); con {@code ddl-auto=validate} Hibernate solo comprueba al arrancar que la
 * entidad y la tabla coincidan.</p>
 *
 * <p>Relaciones:</p>
 * <ul>
 *   <li>Muchos eventos pertenecen a UN {@link Venue} (N:1). Este es el lado dueño: tiene la clave foránea {@code venue_id}.</li>
 *   <li>Muchos a muchos con {@link Artist}: este es el lado dueño; la tabla intermedia es {@code event_artists}.</li>
 *   <li>Un evento tiene MUCHOS {@link Ticket} (1:N, lado inverso).</li>
 * </ul>
 * <p>Índices en la base: {@code venue_id}, {@code status} y {@code event_date}.</p>
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código de negocio único (máx. 30 caracteres): es como los clientes identifican el evento en la API (BR-EVENT-001).
    @Column(name = "event_code", nullable = false, unique = true)
    private String eventCode;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    // Solo fecha (LocalDate -> DATE), sin hora.
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    // @Enumerated(STRING) guarda el NOMBRE del enum ("MUSIC") y no su posición numérica: así no se rompe si se reordena el enum.
    // La base tiene un CHECK con los valores permitidos (chk_event_category).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    // Estado del ciclo de vida (ver EventStatus). También protegido por un CHECK en la base (chk_event_status).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    // Edad mínima para asistir; 0 o null = sin restricción. La valida TicketServiceImpl al comprar (BR-TICKET-006).
    @Column(name = "minimum_age")
    private Integer minimumAge;

    // Columna agregada en una migración posterior (V3, hasta 500 caracteres, opcional). Hoy solo la usan las pruebas;
    // ningún DTO ni servicio la expone.
    @Column(name = "streaming_url")
    private String streamingUrl;

    // Lado DUEÑO del N:1: @JoinColumn crea la clave foránea venue_id (obligatoria, apunta a venues.id).
    // LAZY: al cargar un evento NO se carga su recinto hasta que se accede a él (por eso los mappers se ejecutan
    // dentro de la transacción del servicio).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    // Lado DUEÑO del muchos-a-muchos: @JoinTable describe la tabla intermedia event_artists con sus dos claves
    // foráneas (event_id y artist_id). Su clave primaria compuesta (event_id, artist_id) impide en la base que un
    // mismo artista se asigne dos veces al mismo evento (BR-EVENT-010 lo valida además en el servicio).
    // Set: sin duplicados. @ManyToMany es LAZY por defecto.
    @ManyToMany
    @JoinTable(
            name = "event_artists",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<Artist> artists = new HashSet<>();

    // Lado INVERSO: la clave foránea está en Ticket.event. Sirve para navegar de evento a sus tickets.
    @OneToMany(mappedBy = "event")
    private List<Ticket> tickets = new ArrayList<>();

    // Constructor sin argumentos: lo exige JPA (Hibernate crea las entidades por reflexión). Es protected
    // para que el resto del código no pueda crear una entidad vacía.
    protected Event() {
    }

    // Constructor de negocio. El recinto no se pasa aquí: se asigna después con setVenue.
    public Event(String eventCode, String name, String description, LocalDate eventDate,
                 EventCategory category, EventStatus status, Integer minimumAge) {
        this.eventCode = eventCode;
        this.name = name;
        this.description = description;
        this.eventDate = eventDate;
        this.category = category;
        this.status = status;
        this.minimumAge = minimumAge;
    }

    // Asigna el recinto (lado dueño de la relación: es lo que realmente escribe venue_id).
    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    // Único punto de cambio del estado. Lo llaman publish (-> PUBLISHED) y purchase (-> SOLD_OUT).
    public void changeStatus(EventStatus status) {
        this.status = status;
    }


    // Mantiene sincronizados los DOS lados de la relación: agrega el artista al evento (lado dueño, el que escribe
    // la fila en event_artists) y el evento al artista (lado inverso, solo en memoria).
    public void addArtist(Artist artist) {
        artists.add(artist);
        artist.getEvents().add(this);
    }

    public Long getId() {
        return id;
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public EventCategory getCategory() {
        return category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Integer getMinimumAge() {
        return minimumAge;
    }

    public String getStreamingUrl() {
        return streamingUrl;
    }

    // Setter de la columna agregada en V3.
    public void setStreamingUrl(String streamingUrl) {
        this.streamingUrl = streamingUrl;
    }

    public Venue getVenue() {
        return venue;
    }

    public Set<Artist> getArtists() {
        return artists;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }
}