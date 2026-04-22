package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.recrutement.ApplicationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, Long> {
    List<ApplicationAnswer> findByApplicationId(Long applicationId);
    void deleteByQuestionId(Long questionId);
}