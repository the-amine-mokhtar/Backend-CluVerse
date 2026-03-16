package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import org.springframework.stereotype.Service;

@Service
public class ClubService extends EntityServiceImpl<Club, Long> {
    public ClubService(ClubRepository repository) {
        super(repository);
    }
}

