package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Election;
import com.hexaweb.backendcluverse.entities.ElectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ElectionRepository extends JpaRepository<Election, Long> {
    List<Election> findByPositionClubId(Long clubId);
    List<Election> findByPositionId(Long positionId);
    boolean existsByPositionIdAndStatusIn(Long positionId, List<ElectionStatus> statuses);
}
