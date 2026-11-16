package com.pulsepass.repository;

import com.pulsepass.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos de {@link User}.
 *
 * <p><b>Repositorio Spring Data JPA:</b> es solo una INTERFAZ; Spring genera la implementación al arrancar. Al extender {@code JpaRepository<Entidad, TipoDelId>} ya incluye las operaciones básicas (save, findById, findAll, deleteById, count...). Las consultas propias se definen de dos formas: derivadas del NOMBRE del método ({@code findBy...}, {@code existsBy...}, {@code countBy...}; con palabras como And, IgnoreCase, OrderBy...Asc y navegando relaciones, p. ej. {@code findByVenueCode} = venue.code) o escritas con {@code @Query} en JPQL (consultas sobre entidades y sus campos, no sobre tablas).</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // Buscar usuario por email ignorando mayúsculas/minúsculas (sección 14)
    // Optional: vacío si no existe. La usan TicketServiceImpl (comprador), UserServiceImpl y AuthorizationServiceImpl.
    Optional<User> findByEmailIgnoreCase(String email);

    // Búsqueda exacta por username.
    Optional<User> findByUsername(String username);

    // Preguntan solo si existe (sin traer la entidad): se usan para validar unicidad al registrar (BR-USER-001 y 002).
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

}
