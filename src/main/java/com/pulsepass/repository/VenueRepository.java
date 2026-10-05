package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    // FR-VEN-001: buscar venue por su código de negocio (no el ID técnico)
    Optional<Venue> findByCode(String code);
}
