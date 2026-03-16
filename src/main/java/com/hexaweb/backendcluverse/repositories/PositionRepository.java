package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {
}

