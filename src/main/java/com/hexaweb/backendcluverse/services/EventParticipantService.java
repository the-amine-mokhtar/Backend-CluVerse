package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.repositories.EventParticipantRepository;
import org.springframework.stereotype.Service;

@Service
public class EventParticipantService extends EntityServiceImpl<EventParticipant, Long> {
    public EventParticipantService(EventParticipantRepository repository) {
        super(repository);
    }
}

