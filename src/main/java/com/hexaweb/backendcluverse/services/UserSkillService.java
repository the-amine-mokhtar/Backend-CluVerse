package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.UserSkill;
import com.hexaweb.backendcluverse.entities.UserSkillId;
import com.hexaweb.backendcluverse.repositories.UserSkillRepository;
import org.springframework.stereotype.Service;

@Service
public class UserSkillService extends EntityServiceImpl<UserSkill, UserSkillId> {
    public UserSkillService(UserSkillRepository repository) {
        super(repository);
    }
}

