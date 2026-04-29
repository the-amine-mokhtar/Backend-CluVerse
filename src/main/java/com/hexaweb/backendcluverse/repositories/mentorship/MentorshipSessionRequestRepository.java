package com.hexaweb.backendcluverse.repositories.mentorship;

import com.hexaweb.backendcluverse.entities.mentorship.MentorshipSessionRequest;
import com.hexaweb.backendcluverse.entities.mentorship.MentorshipSessionRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorshipSessionRequestRepository extends JpaRepository<MentorshipSessionRequest, Long> {
    
    List<MentorshipSessionRequest> findByRequestedUserIdAndStatus(Long userId, RequestStatus status);
    
    List<MentorshipSessionRequest> findByRequesterId(Long userId);
    
    Optional<MentorshipSessionRequest> findByConversationIdAndStatus(
        Long conversationId, RequestStatus status);
}
