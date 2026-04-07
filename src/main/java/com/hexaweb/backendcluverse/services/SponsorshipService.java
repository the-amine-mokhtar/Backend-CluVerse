package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsorship;
import com.hexaweb.backendcluverse.repositories.SponsorshipRepository;
import org.springframework.stereotype.Service;

@Service
public class SponsorshipService extends EntityServiceImpl<Sponsorship, Long> {
    public SponsorshipService(SponsorshipRepository repository) {
        super(repository);
    }
}

