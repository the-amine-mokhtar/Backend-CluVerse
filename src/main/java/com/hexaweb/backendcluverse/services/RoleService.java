package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Role;
import com.hexaweb.backendcluverse.repositories.RoleRepository;
import org.springframework.stereotype.Service;

@Service
public class RoleService extends EntityServiceImpl<Role, Long> {
    public RoleService(RoleRepository repository) {
        super(repository);
    }
}

