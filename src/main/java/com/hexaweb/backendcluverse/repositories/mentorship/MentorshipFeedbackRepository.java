package com.hexaweb.backendcluverse.repositories.mentorship;

import com.hexaweb.backendcluverse.entities.mentorship.MentorshipFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorshipFeedbackRepository extends JpaRepository<MentorshipFeedback, Long> {
    
    List<MentorshipFeedback> findByConversationId(Long conversationId);
    
    Optional<MentorshipFeedback> findByConversationIdAndGiverId(Long conversationId, Long giverId);
    
    List<MentorshipFeedback> findByReceiverId(Long receiverId);
}
