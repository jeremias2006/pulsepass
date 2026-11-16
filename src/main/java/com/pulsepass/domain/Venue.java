package com.pulsepass.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Recinto donde se realizan los eventos. Tabla {@code venues} (migración V1).
 *
 * <p><b>Entidad JPA:</b> cada instancia es una fila de la tabla indicada. {@code @Id} junto con
 * {@code @GeneratedValue(IDENTITY)} significa que la clave primaria la genera la base de datos
 * (BIGSERIAL en PostgreSQL). El constructor protegido sin argumentos lo exige JPA (Hibernate lo usa
 * por reflexión); el constructor público es el que usa nuestro código. Casi no hay setters: los
 * cambios pasan por métodos con nombre propio. El esquema real lo crean las migraciones de Flyway
 * ({@code db/migration}); con {@code ddl-auto=validate} Hibernate solo comprueba al arrancar que la
 * entidad y la tabla coincidan.</p>
 *
 * <p>Relación: un Venue tiene MUCHOS eventos (1:N). Este es el lado inverso; el lado dueño, con la
 * clave foránea {@code venue_id}, está en {@link Event}.</p>
 */
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código de negocio único (máx. 20 caracteres). Los clientes identifican el recinto con este código, no con el id.
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column
    private String address;

    // Aforo máximo: BR-TICKET-007 compara los tickets PAID del evento contra este valor.
    // En la base hay además un CHECK (capacity > 0).
    @Column(nullable = false)
    private Integer capacity;

    // Un recinto inactivo no permite crear ni publicar eventos (BR-EVENT-003 y BR-EVENT-009).
    @Column(nullable = false)
    private boolean active = true;

    // Lado INVERSO de la relación 1:N: mappedBy = "venue" indica que la clave foránea vive en Event.venue.
    // Aquí no hay columna; esta lista es solo para navegar de recinto a eventos. Los @OneToMany son LAZY por defecto.
    @OneToMany(mappedBy = "venue")
    private List<Event> events = new ArrayList<>();

    // Constructor sin argumentos: lo exige JPA (Hibernate crea las entidades por reflexión). Es protected
    // para que el resto del código no pueda crear una entidad vacía.
    protected Venue() {
    }

    // Constructor de negocio: no recibe id (lo genera la BD) ni active (nace en true).
    public Venue(String code, String name, String city, String address, Integer capacity) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.address = address;
        this.capacity = capacity;
    }

    // Mantiene sincronizados los DOS lados de la relación (lista del Venue y venue del Event).
    // El servicio actual asigna el recinto con event.setVenue(...) directamente.
    public void addEvent(Event event) {
        events.add(event);
        event.setVenue(this);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public boolean isActive() {
        return active;
    }

    public List<Event> getEvents() {
        return events;
    }
}