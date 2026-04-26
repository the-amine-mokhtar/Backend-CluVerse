package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.recrutement.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, String> {
    List<InterviewSession> findByPositionId(Long positionId);
    List<InterviewSession> findByMemberId(Long memberId);
}