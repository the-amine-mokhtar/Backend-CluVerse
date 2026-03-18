package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Candidate;
import com.hexaweb.backendcluverse.entities.CandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByElectionId(Long electionId);
    boolean existsByUserIdAndElectionId(Long userId, Long electionId);
    boolean existsByUserIdAndElectionIdAndStatusIn(Long userId, Long electionId, List<CandidateStatus> statuses);
}
