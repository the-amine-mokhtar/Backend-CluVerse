package com.hexaweb.backendcluverse.repositories.mentorship;

import com.hexaweb.backendcluverse.entities.mentorship.MentorshipGoal;
import com.hexaweb.backendcluverse.entities.mentorship.MentorshipGoal.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorshipGoalRepository extends JpaRepository<MentorshipGoal, Long> {
    
    List<MentorshipGoal> findByConversationId(Long conversationId);
    
    List<MentorshipGoal> findByMenteeId(Long menteeId);
    
    List<MentorshipGoal> findByMenteeIdAndStatus(Long menteeId, GoalStatus status);
}
