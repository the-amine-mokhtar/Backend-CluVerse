package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.election.InterviewConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InterviewConfigRepository extends JpaRepository<InterviewConfig, Long> {
    Optional<InterviewConfig> findByUniqueLink(String uniqueLink);
    Optional<InterviewConfig> findTopByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}