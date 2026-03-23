package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.election.VacantPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VacantPositionRepository extends JpaRepository<VacantPosition, Long> {
    List<VacantPosition> findByClubId(Long clubId);
}