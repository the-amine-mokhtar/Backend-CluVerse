package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}

