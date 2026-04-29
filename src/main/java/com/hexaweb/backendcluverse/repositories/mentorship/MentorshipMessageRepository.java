package com.hexaweb.backendcluverse.repositories.mentorship;

import com.hexaweb.backendcluverse.entities.mentorship.MentorshipMessage;
import com.hexaweb.backendcluverse.entities.mentorship.MentorshipConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorshipMessageRepository extends JpaRepository<MentorshipMessage, Long> {
    
    List<MentorshipMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);
    
    List<MentorshipMessage> findByConversationIdAndIsReadFalse(Long conversationId);
}
