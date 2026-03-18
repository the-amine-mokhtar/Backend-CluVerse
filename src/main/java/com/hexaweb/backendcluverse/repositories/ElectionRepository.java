package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Election;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ElectionRepository extends JpaRepository<Election, Long> {
    List<Election> findByPositionClubId(Long clubId);
    List<Election> findByPositionId(Long positionId);
}
