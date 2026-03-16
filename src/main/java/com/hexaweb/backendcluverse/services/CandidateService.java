package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Candidate;
import com.hexaweb.backendcluverse.repositories.CandidateRepository;
import org.springframework.stereotype.Service;

@Service
public class CandidateService extends EntityServiceImpl<Candidate, Long> {
    public CandidateService(CandidateRepository repository) {
        super(repository);
    }
}

