package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.skills.Skill;
import com.hexaweb.backendcluverse.repositories.SkillRepository;
import org.springframework.stereotype.Service;

@Service
public class SkillService extends EntityServiceImpl<Skill, Long> {
    public SkillService(SkillRepository repository) {
        super(repository);
    }
}

