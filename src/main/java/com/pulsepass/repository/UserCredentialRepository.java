package com.pulsepass.repository;

import com.pulsepass.domain.UserCredential;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

    // Un solo SELECT con JOIN a users y user_roles (evita N+1, NFR-SEC003)
    @EntityGraph(attributePaths = {"user", "roles"})
    Optional<UserCredential> findByUserEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"user", "roles"})
    Optional<UserCredential> findByUserUsername(String username);

    @EntityGraph(attributePaths = {"user", "roles"})
    Optional<UserCredential> findByUserId(Long userId);
}