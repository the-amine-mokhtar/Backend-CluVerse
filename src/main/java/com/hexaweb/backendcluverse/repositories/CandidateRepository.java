package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
}

