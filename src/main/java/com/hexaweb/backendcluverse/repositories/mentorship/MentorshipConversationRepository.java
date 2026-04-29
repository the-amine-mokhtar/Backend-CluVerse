package com.hexaweb.backendcluverse.repositories.mentorship;

import com.hexaweb.backendcluverse.entities.mentorship.MentorshipConversation;
import com.hexaweb.backendcluverse.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorshipConversationRepository extends JpaRepository<MentorshipConversation, Long> {
    
    List<MentorshipConversation> findByMentorIdAndActiveTrue(Long mentorId);
    
    List<MentorshipConversation> findByMenteeIdAndActiveTrue(Long menteeId);
    
    Optional<MentorshipConversation> findByMentorIdAndMenteeIdAndSkillNameAndActiveTrue(
        Long mentorId, Long menteeId, String skillName
    );
    
    List<MentorshipConversation> findByClubIdAndActiveTrue(Long clubId);
}
