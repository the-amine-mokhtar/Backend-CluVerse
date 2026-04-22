package com.hexaweb.backendcluverse.repositories.Competencies;

import com.hexaweb.backendcluverse.entities.competencies.CompetencySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetencySessionRepository extends JpaRepository<CompetencySession, Long> {

    List<CompetencySession> findByClubIdOrderByStartsAtDesc(Long clubId);
}
