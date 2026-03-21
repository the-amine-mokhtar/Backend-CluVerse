package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.recruitement.ApplicationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, Long> {
    List<ApplicationAnswer> findByApplicationId(Long applicationId);
}