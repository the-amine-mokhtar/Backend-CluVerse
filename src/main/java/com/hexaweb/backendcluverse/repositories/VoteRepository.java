package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.election.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByVoterIdAndElectionId(Long voterId, Long electionId);
    List<Vote> findByElectionId(Long electionId);
}
