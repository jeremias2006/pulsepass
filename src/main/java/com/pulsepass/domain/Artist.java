package com.pulsepass.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Artista que participa en eventos. Tabla {@code artists} (migración V1; V2 inserta 5 artistas iniciales).
 *
 * <p><b>Entidad JPA:</b> cada instancia es una fila de la tabla indicada. {@code @Id} junto con
 * {@code @GeneratedValue(IDENTITY)} significa que la clave primaria la genera la base de datos
 * (BIGSERIAL en PostgreSQL). El constructor protegido sin argumentos lo exige JPA (Hibernate lo usa
 * por reflexión); el constructor público es el que usa nuestro código. Casi no hay setters: los
 * cambios pasan por métodos con nombre propio. El esquema real lo crean las migraciones de Flyway
 * ({@code db/migration}); con {@code ddl-auto=validate} Hibernate solo comprueba al arrancar que la
 * entidad y la tabla coincidan.</p>
 *
 * <p>Relación: muchos a muchos con {@link Event}. Este es el lado inverso; el lado dueño (tabla
 * intermedia {@code event_artists}) está en {@code Event.artists}.</p>
 */
@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre artístico, único (FR-ART-002). La columna se llama stage_name; JPA la enlaza con este campo mediante name = ...
    @Column(name = "stage_name", nullable = false, unique = true)
    private String stageName;

    @Column
    private String country;

    @Column
    private String genre;

    @Column(nullable = false)
    private boolean active = true;

    // Lado INVERSO del muchos-a-muchos: mappedBy = "artists" apunta al campo dueño en Event. No genera columnas.
    // Se usa Set (no List) porque un artista no se repite dentro de un mismo evento.
    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new HashSet<>();

    // Constructor sin argumentos: lo exige JPA (Hibernate crea las entidades por reflexión). Es protected
    // para que el resto del código no pueda crear una entidad vacía.
    protected Artist() {
    }

    public Artist(String stageName, String country, String genre) {
        this.stageName = stageName;
        this.country = country;
        this.genre = genre;
    }

    public Long getId() {
        return id;
    }

    public String getStageName() {
        return stageName;
    }

    public String getCountry() {
        return country;
    }

    public String getGenre() {
        return genre;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Event> getEvents() {
        return events;
    }
}