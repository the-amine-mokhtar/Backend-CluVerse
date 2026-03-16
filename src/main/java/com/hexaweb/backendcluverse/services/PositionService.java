package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Position;
import com.hexaweb.backendcluverse.repositories.PositionRepository;
import org.springframework.stereotype.Service;

@Service
public class PositionService extends EntityServiceImpl<Position, Long> {
    public PositionService(PositionRepository repository) {
        super(repository);
    }
}

