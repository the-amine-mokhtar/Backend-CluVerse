package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService extends EntityServiceImpl<User, Long> {
    public UserService(UserRepository repository) {
        super(repository);
    }
}

