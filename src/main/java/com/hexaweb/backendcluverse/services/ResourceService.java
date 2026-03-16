package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Resource;
import com.hexaweb.backendcluverse.repositories.ResourceRepository;
import org.springframework.stereotype.Service;

@Service
public class ResourceService extends EntityServiceImpl<Resource, Long> {
    public ResourceService(ResourceRepository repository) {
        super(repository);
    }
}

