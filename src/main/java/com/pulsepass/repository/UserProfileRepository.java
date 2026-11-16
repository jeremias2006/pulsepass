package com.pulsepass.repository;

import com.pulsepass.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link UserProfile}.
 *
 * <p><b>Repositorio Spring Data JPA:</b> es solo una INTERFAZ; Spring genera la implementación al arrancar. Al extender {@code JpaRepository<Entidad, TipoDelId>} ya incluye las operaciones básicas (save, findById, findAll, deleteById, count...). Las consultas propias se definen de dos formas: derivadas del NOMBRE del método ({@code findBy...}, {@code existsBy...}, {@code countBy...}; con palabras como And, IgnoreCase, OrderBy...Asc y navegando relaciones, p. ej. {@code findByVenueCode} = venue.code) o escritas con {@code @Query} en JPQL (consultas sobre entidades y sus campos, no sobre tablas).</p>
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    // El PRD no exige consultas propias: se accede vía User.getProfile() (relación 1:1).
    // Solo hereda los métodos de JpaRepository.
}
