package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // FR-TKT-006: tickets de un usuario por email, con status opcional.
    // Dos firmas: la capa de servicio decide cuál llamar según si hay filtro de estado.
    List<Ticket> findByUserEmail(String email);

    List<Ticket> findByUserEmailAndStatus(String email, TicketStatus status);

    // FR-TKT-007: tickets PAID por eventCode.
    // DECISIÓN: Query Method (no JPQL). Es un filtro de igualdad simple sobre dos
    // propiedades navegadas, sin agregaciones ni múltiples joins -> el nombre del
    // método ya es autoexplicativo. A diferencia de FR-SRC-002, que cruza dos
    // asociaciones distintas y por eso sí se resolvió con JPQL.
    List<Ticket> findByEventEventCodeAndStatus(String eventCode, TicketStatus status);

    // FR-TKT-008: conteo de tickets PAID de un evento -> requiere COUNT -> JPQL.
    @Query("""
            SELECT COUNT(t) FROM Ticket t
            WHERE t.event.eventCode = :eventCode
              AND t.status = com.pulsepass.domain.TicketStatus.PAID
            """)
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    // FR-SRC-004 (Could, opcional): tickets cuyo evento sea posterior a una fecha,
    // ordenados cronológicamente por la fecha del evento.
    List<Ticket> findByEventEventDateAfterOrderByEventEventDateAsc(LocalDate date);
}
