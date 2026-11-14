package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de {@link Event}, incluidas las búsquedas por ciudad y artista (FR-SRC-*), que cruzan
 * varias relaciones y por eso se escriben en JPQL.
 *
 * <p><b>Repositorio Spring Data JPA:</b> es solo una INTERFAZ; Spring genera la implementación al arrancar. Al extender {@code JpaRepository<Entidad, TipoDelId>} ya incluye las operaciones básicas (save, findById, findAll, deleteById, count...). Las consultas propias se definen de dos formas: derivadas del NOMBRE del método ({@code findBy...}, {@code existsBy...}, {@code countBy...}; con palabras como And, IgnoreCase, OrderBy...Asc y navegando relaciones, p. ej. {@code findByVenueCode} = venue.code) o escritas con {@code @Query} en JPQL (consultas sobre entidades y sus campos, no sobre tablas).</p>
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    // Solo pregunta si existe (más liviano que traer la entidad). La usa EventServiceImpl.create para BR-EVENT-001.
    boolean existsByEventCode(String eventCode);


    // FR-EVT-002: eventCode es único, Query Method directo
    // Optional: vacío si no existe. La usan EventServiceImpl y TicketServiceImpl.
    Optional<Event> findByEventCode(String eventCode);

    // FR-EVT-005: eventos PUBLISHED ordenados por fecha ascendente
    // La usa EventServiceImpl.findPublishedEvents (con status = PUBLISHED).
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // FR-VEN-004: Query Method navegando la relación Event -> Venue (venue.code)
    // Hoy solo la usan las pruebas (ningún servicio la invoca todavía).
    List<Event> findByVenueCode(String venueCode);

    // FR-SRC-001 / FR-ART-004: JPQL con JOIN, exigido explícitamente por el PRD.
    // DISTINCT porque un evento con varios artistas que coincidan no debe duplicarse.
    // Cómo leer el JPQL: "Event e" es la ENTIDAD con alias e; "JOIN e.artists a" une a través de la relación (en SQL
    // pasa por event_artists); LOWER(...) compara sin distinguir mayúsculas; ":stageName" es un parámetro con nombre
    // que se enlaza con @Param. La usa EventServiceImpl.findByArtist.
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.artists a
            WHERE LOWER(a.stageName) = LOWER(:stageName)
            """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    // FR-SRC-002: eventos de una ciudad en los que participe un artista específico.
    // Cruza DOS asociaciones (venue y artists) -> JPQL, no Query Method.
    // Hoy solo la usan las pruebas.
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE LOWER(v.city) = LOWER(:city)
              AND LOWER(a.stageName) = LOWER(:stageName)
            """)
    List<Event> findByCityAndArtistStageName(@Param("city") String city,
                                             @Param("stageName") String stageName);

    // FR-SRC-003: eventos PUBLISHED, posteriores a una fecha, en una ciudad,
    // y cuyo artista CONTENGA un texto (LIKE). Case-insensitive, DISTINCT, ordenado.
    // LIKE '%texto%' = "contiene" (CONCAT arma el patrón con los comodines). El estado se compara con el enum
    // escrito con su nombre completo de clase. Hoy solo la usan las pruebas.
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE e.status = com.pulsepass.domain.EventStatus.PUBLISHED
              AND e.eventDate > :afterDate
              AND LOWER(v.city) = LOWER(:city)
              AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistKeyword, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> findRecommendedEvents(@Param("afterDate") LocalDate afterDate,
                                      @Param("city") String city,
                                      @Param("artistKeyword") String artistKeyword);
}
