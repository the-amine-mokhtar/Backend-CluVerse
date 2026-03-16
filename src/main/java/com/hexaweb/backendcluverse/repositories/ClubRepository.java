package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Club;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {
}

