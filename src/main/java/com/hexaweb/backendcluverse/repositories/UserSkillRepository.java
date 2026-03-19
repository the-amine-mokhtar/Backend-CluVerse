package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.skills.UserSkill;
import com.hexaweb.backendcluverse.entities.skills.UserSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSkillRepository extends JpaRepository<UserSkill, UserSkillId> {
}

