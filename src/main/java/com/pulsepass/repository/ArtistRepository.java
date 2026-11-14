package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


import java.util.Optional;

/**
 * Acceso a datos de {@link Artist}.
 *
 * <p><b>Repositorio Spring Data JPA:</b> es solo una INTERFAZ; Spring genera la implementación al arrancar. Al extender {@code JpaRepository<Entidad, TipoDelId>} ya incluye las operaciones básicas (save, findById, findAll, deleteById, count...). Las consultas propias se definen de dos formas: derivadas del NOMBRE del método ({@code findBy...}, {@code existsBy...}, {@code countBy...}; con palabras como And, IgnoreCase, OrderBy...Asc y navegando relaciones, p. ej. {@code findByVenueCode} = venue.code) o escritas con {@code @Query} en JPQL (consultas sobre entidades y sus campos, no sobre tablas).</p>
 */
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    // FR-ART-002: stageName es único
    // Búsqueda exacta (distingue mayúsculas). Hoy solo la usan las pruebas; ArtistServiceImpl usa la variante IgnoreCase.
    // Optional: vacío si no existe.
    Optional<Artist> findByStageName(String stageName);

    // Igual, sin distinguir mayúsculas. La usa ArtistServiceImpl.findByStageName.
    Optional<Artist> findByStageNameIgnoreCase(String stageName);

    // ActiveTrue = WHERE active = true; OrderByStageNameAsc = ORDER BY stage_name ASC. La usa ArtistServiceImpl.
    List<Artist> findByActiveTrueOrderByStageNameAsc();

}
