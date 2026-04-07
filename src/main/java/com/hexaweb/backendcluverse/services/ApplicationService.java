package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.recruitement.Application;
import com.hexaweb.backendcluverse.repositories.ApplicationRepository;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService extends EntityServiceImpl<Application, Long> {
    public ApplicationService(ApplicationRepository repository) {
        super(repository);
    }
}

