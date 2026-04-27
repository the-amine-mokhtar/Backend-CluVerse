package com.hexaweb.backendcluverse.repositories.mentorship;

import com.hexaweb.backendcluverse.entities.mentorship.MentorshipCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorshipCertificateRepository extends JpaRepository<MentorshipCertificate, Long> {
    
    Optional<MentorshipCertificate> findByConversationId(Long conversationId);
    
    List<MentorshipCertificate> findByMenteeId(Long menteeId);
    
    List<MentorshipCertificate> findByMentorId(Long mentorId);
}
