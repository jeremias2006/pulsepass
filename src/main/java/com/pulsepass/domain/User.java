package com.pulsepass.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Usuario del sistema (datos de cuenta). Tabla {@code users} (migración V1).
 *
 * <p><b>Entidad JPA:</b> cada instancia es una fila de la tabla indicada. {@code @Id} junto con
 * {@code @GeneratedValue(IDENTITY)} significa que la clave primaria la genera la base de datos
 * (BIGSERIAL en PostgreSQL). El constructor protegido sin argumentos lo exige JPA (Hibernate lo usa
 * por reflexión); el constructor público es el que usa nuestro código. Casi no hay setters: los
 * cambios pasan por métodos con nombre propio. El esquema real lo crean las migraciones de Flyway
 * ({@code db/migration}); con {@code ddl-auto=validate} Hibernate solo comprueba al arrancar que la
 * entidad y la tabla coincidan.</p>
 *
 * <p>Los datos personales están en {@link UserProfile} (relación 1:1) y la contraseña y los roles en
 * {@link UserCredential}, en tablas y entidades aparte: esta clase no contiene ningún dato secreto.</p>
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Único (máx. 50 caracteres). BR-USER-001.
    @Column(nullable = false, unique = true)
    private String username;

    // Único (máx. 150). El UNIQUE de PostgreSQL SÍ distingue mayúsculas; por eso BR-USER-002 se valida además en el
    // servicio con existsByEmailIgnoreCase.
    @Column(nullable = false, unique = true)
    private String email;

    // Cuenta activa o no. Un usuario inactivo no puede iniciar sesión ni autenticarse con un token (SecurityPrincipal.isEnabled).
    @Column(nullable = false)
    private boolean active = true;

    // Lado INVERSO del 1:1 (la clave foránea está en UserProfile.user).
    // cascade = ALL: las operaciones sobre el User (guardar, borrar) se propagan al perfil; por eso basta con
    // userRepository.save(user) para guardar ambos. orphanRemoval: si se desvincula el perfil, se elimina.
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserProfile profile;

    // Lado INVERSO del 1:N (la clave foránea está en Ticket.user).
    @OneToMany(mappedBy = "user")
    private List<Ticket> tickets = new ArrayList<>();

    // Constructor sin argumentos: lo exige JPA (Hibernate crea las entidades por reflexión). Es protected
    // para que el resto del código no pueda crear una entidad vacía.
    protected User() {
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }

    // Enlaza los DOS lados del 1:1: el perfil queda en el usuario y el usuario en el perfil (que es el dueño de la FK).
    public void assignProfile(UserProfile profile) {
        this.profile = profile;
        profile.setUser(this);
    }

    // Desactiva la cuenta (active = false). Hoy no hay un endpoint que lo invoque; lo usan las pruebas de seguridad.
    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }
}