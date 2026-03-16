package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import org.springframework.stereotype.Service;

@Service
public class MembershipService extends EntityServiceImpl<Membership, Long> {
    public MembershipService(MembershipRepository repository) {
        super(repository);
    }
}

