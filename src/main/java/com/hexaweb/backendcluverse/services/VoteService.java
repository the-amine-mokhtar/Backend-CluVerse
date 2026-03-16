package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Vote;
import com.hexaweb.backendcluverse.repositories.VoteRepository;
import org.springframework.stereotype.Service;

@Service
public class VoteService extends EntityServiceImpl<Vote, Long> {
    public VoteService(VoteRepository repository) {
        super(repository);
    }
}

