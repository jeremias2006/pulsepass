package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // FR-EVT-002: eventCode es único, Query Method directo
    Optional<Event> findByEventCode(String eventCode);

    // FR-EVT-005: eventos PUBLISHED ordenados por fecha ascendente
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // FR-VEN-004: Query Method navegando la relación Event -> Venue (venue.code)
    List<Event> findByVenueCode(String venueCode);

    // FR-SRC-001 / FR-ART-004: JPQL con JOIN, exigido explícitamente por el PRD.
    // DISTINCT porque un evento con varios artistas que coincidan no debe duplicarse.
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.artists a
            WHERE LOWER(a.stageName) = LOWER(:stageName)
            """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    // FR-SRC-002: eventos de una ciudad en los que participe un artista específico.
    // Cruza DOS asociaciones (venue y artists) -> JPQL, no Query Method.
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
