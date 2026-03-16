package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Election;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionRepository extends JpaRepository<Election, Long> {
}

