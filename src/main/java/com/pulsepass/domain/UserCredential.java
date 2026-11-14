package com.pulsepass.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Credenciales de acceso de un usuario: hash de contraseña y roles. Tablas {@code user_credentials} y
 * {@code user_roles} (migración V4).
 *
 * <p><b>Entidad JPA:</b> cada instancia es una fila de la tabla indicada. {@code @Id} junto con
 * {@code @GeneratedValue(IDENTITY)} significa que la clave primaria la genera la base de datos
 * (BIGSERIAL en PostgreSQL). El constructor protegido sin argumentos lo exige JPA (Hibernate lo usa
 * por reflexión); el constructor público es el que usa nuestro código. Casi no hay setters: los
 * cambios pasan por métodos con nombre propio. El esquema real lo crean las migraciones de Flyway
 * ({@code db/migration}); con {@code ddl-auto=validate} Hibernate solo comprueba al arrancar que la
 * entidad y la tabla coincidan.</p>
 *
 * <p>Vive aparte de {@link User} y nunca se expone en un DTO. Su clave primaria es la MISMA que la del usuario
 * (ver {@code @MapsId}): hay a lo sumo una credencial por usuario. En la base, las claves foráneas tienen
 * {@code ON DELETE CASCADE}: si se borra el usuario, desaparecen su credencial y sus roles.</p>
 */
@Entity
@Table(name = "user_credentials")
public class UserCredential {

    // Clave primaria COMPARTIDA con users: no se autogenera, toma el valor del id del User (ver @MapsId más abajo).
    @Id
    @Column(name = "user_id")
    private Long userId;

    // @MapsId: la clave foránea user_id es a la vez la clave primaria de esta tabla, y su valor se copia del User
    // asignado. optional = false: no puede existir una credencial sin usuario. LAZY: el User no se carga hasta usarlo.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // Hash BCrypt (60 caracteres; la columna admite 100). NUNCA la contraseña en texto plano. La base rechaza un hash vacío.
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    // Cuándo se fijó la contraseña (se llena al registrar y en el seeder).
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    // @ElementCollection: colección de valores simples (no de entidades). Se guarda en la tabla user_roles
    // (user_id, role) descrita por @CollectionTable; cada fila es un rol. El enum se guarda como texto.
    // La base limita los valores con CHECK (USER, STAFF, ADMIN). LAZY: por eso los repositorios usan @EntityGraph.
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Set<Role> roles = new HashSet<>();

    // Constructor sin argumentos: lo exige JPA (Hibernate crea las entidades por reflexión). Es protected
    // para que el resto del código no pueda crear una entidad vacía.
    protected UserCredential() {
    }

    // Constructor de negocio. No recibe userId: se deriva del User por @MapsId.
    public UserCredential(User user, String passwordHash, Set<Role> roles, LocalDateTime passwordChangedAt) {
        this.user = user;
        this.passwordHash = passwordHash;
        // Copia defensiva: si quien llamó modifica su conjunto después, la entidad no se ve afectada.
        this.roles = new HashSet<>(roles);
        this.passwordChangedAt = passwordChangedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
