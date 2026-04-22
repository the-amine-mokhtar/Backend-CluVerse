package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.skills.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
}

