package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsor;
import com.hexaweb.backendcluverse.repositories.SponsorRepository;
import org.springframework.stereotype.Service;

@Service
public class SponsorService extends EntityServiceImpl<Sponsor, Long> {
    public SponsorService(SponsorRepository repository) {
        super(repository);
    }
}

