package com.hexaweb.backendcluverse.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hexaweb.backendcluverse.entities.election.InterviewMessage;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {
}
