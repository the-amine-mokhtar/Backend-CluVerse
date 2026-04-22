package com.hexaweb.backendcluverse.repositories.Competencies;

import com.hexaweb.backendcluverse.entities.competencies.CompetencySessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompetencySessionParticipantRepository extends JpaRepository<CompetencySessionParticipant, Long> {

    List<CompetencySessionParticipant> findBySession_Id(Long sessionId);

    @Query("select p from CompetencySessionParticipant p where p.userId = :userId and p.session.clubId = :clubId")
    List<CompetencySessionParticipant> findByClubIdAndUserId(@Param("clubId") Long clubId, @Param("userId") Long userId);
}
