package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {
}

