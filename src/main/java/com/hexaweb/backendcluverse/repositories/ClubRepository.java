package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    Optional<Club> findByActivationCode(String activationCode);
    boolean existsByEmail(String email);
}

