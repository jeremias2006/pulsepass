package com.pulsepass.repository;

import com.pulsepass.domain.UserCredential;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos de {@link UserCredential}. El tipo del id es {@code Long} porque la clave primaria de la
 * credencial es el id del usuario. La usan {@code CustomUserDetailsService} (login y filtro JWT) y {@code AuthService}.
 *
 * <p><b>Repositorio Spring Data JPA:</b> es solo una INTERFAZ; Spring genera la implementación al arrancar. Al extender {@code JpaRepository<Entidad, TipoDelId>} ya incluye las operaciones básicas (save, findById, findAll, deleteById, count...). Las consultas propias se definen de dos formas: derivadas del NOMBRE del método ({@code findBy...}, {@code existsBy...}, {@code countBy...}; con palabras como And, IgnoreCase, OrderBy...Asc y navegando relaciones, p. ej. {@code findByVenueCode} = venue.code) o escritas con {@code @Query} en JPQL (consultas sobre entidades y sus campos, no sobre tablas).</p>
 */
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

    // Un solo SELECT con JOIN a users y user_roles (evita N+1, NFR-SEC003)
    // @EntityGraph le indica a JPA que cargue AHORA las relaciones LAZY "user" y "roles" en la misma consulta.
    // Sin esto habría una consulta para la credencial y una más por cada relación (el problema "N+1").
    @EntityGraph(attributePaths = {"user", "roles"})
    Optional<UserCredential> findByUserEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"user", "roles"})
    Optional<UserCredential> findByUserUsername(String username);

    @EntityGraph(attributePaths = {"user", "roles"})
    Optional<UserCredential> findByUserId(Long userId);
}