package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import java.util.Optional;

/**
 * Acceso a datos de {@link Venue}.
 *
 * <p><b>Repositorio Spring Data JPA:</b> es solo una INTERFAZ; Spring genera la implementación al arrancar. Al extender {@code JpaRepository<Entidad, TipoDelId>} ya incluye las operaciones básicas (save, findById, findAll, deleteById, count...). Las consultas propias se definen de dos formas: derivadas del NOMBRE del método ({@code findBy...}, {@code existsBy...}, {@code countBy...}; con palabras como And, IgnoreCase, OrderBy...Asc y navegando relaciones, p. ej. {@code findByVenueCode} = venue.code) o escritas con {@code @Query} en JPQL (consultas sobre entidades y sus campos, no sobre tablas).</p>
 */
public interface VenueRepository extends JpaRepository<Venue, Long> {

    // FR-VEN-001: buscar venue por su código de negocio (no el ID técnico)
    Optional<Venue> findByCode(String code);

    // ActiveTrue = WHERE active = true; OrderByNameAsc = ORDER BY name ASC. La usa VenueServiceImpl.
    List<Venue> findByActiveTrueOrderByNameAsc();

}
