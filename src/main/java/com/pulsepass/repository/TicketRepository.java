package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

import java.util.Optional;


/**
 * Acceso a datos de {@link Ticket}. Varias consultas navegan la relación hacia el usuario o el evento
 * (p. ej. {@code findByEventEventCodeAndStatus} = event.eventCode y status).
 *
 * <p><b>Repositorio Spring Data JPA:</b> es solo una INTERFAZ; Spring genera la implementación al arrancar. Al extender {@code JpaRepository<Entidad, TipoDelId>} ya incluye las operaciones básicas (save, findById, findAll, deleteById, count...). Las consultas propias se definen de dos formas: derivadas del NOMBRE del método ({@code findBy...}, {@code existsBy...}, {@code countBy...}; con palabras como And, IgnoreCase, OrderBy...Asc y navegando relaciones, p. ej. {@code findByVenueCode} = venue.code) o escritas con {@code @Query} en JPQL (consultas sobre entidades y sus campos, no sobre tablas).</p>
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Búsqueda por el código de negocio. La usa TicketServiceImpl (consultar, cancelar, usar).
    Optional<Ticket> findByTicketCode(String ticketCode);

    // Tickets de un usuario (navega ticket.user.email), del más reciente al más antiguo. La usa TicketServiceImpl.findByUserEmail.
    List<Ticket> findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(String email);

    // Cuenta tickets de un evento en un estado dado. TicketServiceImpl.purchase la llama con PAID para el control de cupo
    // (BR-TICKET-007).
    long countByEventEventCodeAndStatus(String eventCode, TicketStatus status);


    // FR-TKT-006: tickets de un usuario por email, con status opcional.
    // Dos firmas: la capa de servicio decide cuál llamar según si hay filtro de estado.
    // Hoy solo la usan las pruebas (el servicio usa la variante ordenada por fecha).
    List<Ticket> findByUserEmail(String email);

    List<Ticket> findByUserEmailAndStatus(String email, TicketStatus status);

    // FR-TKT-007: tickets PAID por eventCode.
    // DECISIÓN: Query Method (no JPQL). Es un filtro de igualdad simple sobre dos
    // propiedades navegadas, sin agregaciones ni múltiples joins -> el nombre del
    // método ya es autoexplicativo. A diferencia de FR-SRC-002, que cruza dos
    // asociaciones distintas y por eso sí se resolvió con JPQL.
    // La usa TicketServiceImpl.findPaidTicketsByEvent (con status = PAID).
    List<Ticket> findByEventEventCodeAndStatus(String eventCode, TicketStatus status);

    // FR-TKT-008: conteo de tickets PAID de un evento -> requiere COUNT -> JPQL.
    // Equivale a countByEventEventCodeAndStatus(eventCode, PAID). El servicio usa la versión derivada; esta la
    // ejercitan las pruebas.
    @Query("""
            SELECT COUNT(t) FROM Ticket t
            WHERE t.event.eventCode = :eventCode
              AND t.status = com.pulsepass.domain.TicketStatus.PAID
            """)
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    // ¿Existe un ticket con ese código que pertenezca a ese usuario? La usa AuthorizationServiceImpl.isTicketOwner.
    boolean existsByTicketCodeAndUserId(String ticketCode, Long userId);

    // FR-SRC-004 (Could, opcional): tickets cuyo evento sea posterior a una fecha,
    // ordenados cronológicamente por la fecha del evento.
    // Navega ticket.event.eventDate. Hoy solo la usan las pruebas.
    List<Ticket> findByEventEventDateAfterOrderByEventEventDateAsc(LocalDate date);
}
