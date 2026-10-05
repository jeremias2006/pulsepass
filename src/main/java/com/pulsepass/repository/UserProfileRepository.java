package com.pulsepass.repository;

import com.pulsepass.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    // El PRD no exige consultas propias: se accede vía User.getProfile() (relación 1:1).
    // Solo hereda los métodos de JpaRepository.
}
