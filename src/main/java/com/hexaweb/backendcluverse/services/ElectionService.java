package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Election;
import com.hexaweb.backendcluverse.repositories.ElectionRepository;
import org.springframework.stereotype.Service;

@Service
public class ElectionService extends EntityServiceImpl<Election, Long> {
    public ElectionService(ElectionRepository repository) {
        super(repository);
    }
}

