package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import org.springframework.stereotype.Service;

@Service
public class EventService extends EntityServiceImpl<Event, Long> {
    public EventService(EventRepository repository) {
        super(repository);
    }
}

