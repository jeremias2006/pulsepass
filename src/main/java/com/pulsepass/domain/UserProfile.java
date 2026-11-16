package com.pulsepass.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Datos personales de un usuario (nombre, teléfono, ciudad, fecha de nacimiento). Tabla
 * {@code user_profiles} (migración V1).
 *
 * <p><b>Entidad JPA:</b> cada instancia es una fila de la tabla indicada. {@code @Id} junto con
 * {@code @GeneratedValue(IDENTITY)} significa que la clave primaria la genera la base de datos
 * (BIGSERIAL en PostgreSQL). El constructor protegido sin argumentos lo exige JPA (Hibernate lo usa
 * por reflexión); el constructor público es el que usa nuestro código. Casi no hay setters: los
 * cambios pasan por métodos con nombre propio. El esquema real lo crean las migraciones de Flyway
 * ({@code db/migration}); con {@code ddl-auto=validate} Hibernate solo comprueba al arrancar que la
 * entidad y la tabla coincidan.</p>
 *
 * <p>Relación 1:1 con {@link User}. Este es el lado DUEÑO: tiene la clave foránea {@code user_id}, que es
 * {@code UNIQUE} y {@code NOT NULL}; el UNIQUE es lo que garantiza en la base que un usuario tenga a lo sumo un perfil.</p>
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column
    private String phone;

    @Column
    private String city;

    // Opcional en la base, pero obligatoria para registrarse (BR-USER-005). Con ella se calcula la edad (BR-TICKET-006).
    @Column(name = "birth_date")
    private LocalDate birthDate;

    // Dueño del 1:1: crea la columna user_id (obligatoria y única). Se llena al llamar User.assignProfile(...).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Constructor sin argumentos: lo exige JPA (Hibernate crea las entidades por reflexión). Es protected
    // para que el resto del código no pueda crear una entidad vacía.
    protected UserProfile() {
    }

    public UserProfile(String firstName, String lastName, String phone, String city, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.city = city;
        this.birthDate = birthDate;
    }

    // Lo invoca User.assignProfile para completar el enlace bidireccional.
    public void setUser(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public User getUser() {
        return user;
    }
}