package com.hexaweb.backendcluverse.repositories.Competencies;

import com.hexaweb.backendcluverse.entities.competencies.CompetencySession;
import com.hexaweb.backendcluverse.enumerations.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CompetencySessionRepository extends JpaRepository<CompetencySession, Long> {

    List<CompetencySession> findByClubIdOrderByStartsAtDesc(Long clubId);

    List<CompetencySession> findByStatusAndStartsAtBetween(SessionStatus status, LocalDateTime startInclusive, LocalDateTime endInclusive);
}
