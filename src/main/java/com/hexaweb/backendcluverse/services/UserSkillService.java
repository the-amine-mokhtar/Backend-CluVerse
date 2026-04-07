package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.skills.UserSkill;
import com.hexaweb.backendcluverse.entities.skills.UserSkillId;
import com.hexaweb.backendcluverse.repositories.UserSkillRepository;
import org.springframework.stereotype.Service;

@Service
public class UserSkillService extends EntityServiceImpl<UserSkill, UserSkillId> {
    public UserSkillService(UserSkillRepository repository) {
        super(repository);
    }
}

