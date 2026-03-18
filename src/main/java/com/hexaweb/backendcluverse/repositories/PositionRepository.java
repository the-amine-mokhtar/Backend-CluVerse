package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByClubId(Long clubId);
}
