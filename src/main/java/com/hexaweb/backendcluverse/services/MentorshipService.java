package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.mentorship.MentorshipConversationDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipMessageDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipSessionRequestDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipFeedbackDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipGoalDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipCertificateDto;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.mentorship.MentorshipCertificate;
import com.hexaweb.backendcluverse.entities.mentorship.MentorshipConversation;
import com.hexaweb.backendcluverse.entities.mentorship.MentorshipGoal;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.mentorship.MentorshipMessage;
import com.hexaweb.backendcluverse.entities.mentorship.MentorshipSessionRequest;
import com.hexaweb.backendcluverse.entities.mentorship.MentorshipFeedback;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.mentorship.MentorshipCertificateRepository;
import com.hexaweb.backendcluverse.repositories.mentorship.MentorshipConversationRepository;
import com.hexaweb.backendcluverse.repositories.mentorship.MentorshipGoalRepository;
import com.hexaweb.backendcluverse.repositories.mentorship.MentorshipMessageRepository;
import com.hexaweb.backendcluverse.repositories.mentorship.MentorshipSessionRequestRepository;
import com.hexaweb.backendcluverse.repositories.mentorship.MentorshipFeedbackRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.websocket.MentorshipWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MentorshipService {

    private final MentorshipConversationRepository conversationRepository;
    private final MentorshipMessageRepository messageRepository;
    private final MentorshipSessionRequestRepository sessionRequestRepository;
    private final MentorshipFeedbackRepository feedbackRepository;
    private final MentorshipGoalRepository goalRepository;
    private final MentorshipCertificateRepository certificateRepository;
    private final MentorshipWebSocketHandler webSocketHandler;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final CertificatePdfService certificatePdfService;

    public MentorshipService(
            MentorshipConversationRepository conversationRepository,
            MentorshipMessageRepository messageRepository,
            MentorshipSessionRequestRepository sessionRequestRepository,
            MentorshipFeedbackRepository feedbackRepository,
            MentorshipGoalRepository goalRepository,
            MentorshipCertificateRepository certificateRepository,
            MentorshipWebSocketHandler webSocketHandler,
            UserRepository userRepository,
            ClubRepository clubRepository,
            CertificatePdfService certificatePdfService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.sessionRequestRepository = sessionRequestRepository;
        this.feedbackRepository = feedbackRepository;
        this.goalRepository = goalRepository;
        this.certificateRepository = certificateRepository;
        this.webSocketHandler = webSocketHandler;
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
        this.certificatePdfService = certificatePdfService;
    }

    @Transactional
    public MentorshipConversationDto createConversation(Long mentorId, Long menteeId, Long clubId, 
            String skillName, Integer mentorLevel, Integer menteeLevel) {
        
        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
        User mentee = userRepository.findById(menteeId)
                .orElseThrow(() -> new RuntimeException("Mentee not found"));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));

        Optional<MentorshipConversation> existing = conversationRepository
                .findByMentorIdAndMenteeIdAndSkillNameAndActiveTrue(mentorId, menteeId, skillName);
        
        if (existing.isPresent()) {
            return convertToDto(existing.get());
        }

        MentorshipConversation conversation = new MentorshipConversation();
        conversation.setMentor(mentor);
        conversation.setMentee(mentee);
        conversation.setClub(club);
        conversation.setSkillName(skillName);
        conversation.setMentorLevel(mentorLevel);
        conversation.setMenteeLevel(menteeLevel);
        
        MentorshipConversation saved = conversationRepository.save(conversation);
        return convertToDto(saved);
    }

    @Transactional
    public MentorshipMessageDto sendMessage(Long conversationId, Long senderId, String content) {
        MentorshipConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        MentorshipMessage message = new MentorshipMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        
        MentorshipMessage saved = messageRepository.save(message);
        conversation.addMessage(saved);
        conversationRepository.save(conversation);
        
        // Send WebSocket notification to the other user
        Long otherUserId = conversation.getMentor().getId().equals(senderId) 
            ? conversation.getMentee().getId() 
            : conversation.getMentor().getId();
        
        webSocketHandler.notifyUser(otherUserId, Map.of(
            "type", "new_message",
            "conversationId", conversationId,
            "senderName", sender.getFirstName() + " " + sender.getLastName(),
            "content", content,
            "sentAt", saved.getSentAt().toString()
        ));
        
        return convertToDto(saved);
    }

    public List<MentorshipConversationDto> getUserConversations(Long userId) {
        List<MentorshipConversation> asMentor = conversationRepository.findByMentorIdAndActiveTrue(userId);
        List<MentorshipConversation> asMentee = conversationRepository.findByMenteeIdAndActiveTrue(userId);
        
        asMentee.removeAll(asMentor);
        asMentor.addAll(asMentee);
        
        return asMentor.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<MentorshipMessageDto> getConversationMessages(Long conversationId) {
        List<MentorshipMessage> messages = messageRepository
                .findByConversationIdOrderBySentAtAsc(conversationId);
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(Long conversationId, Long userId) {
        List<MentorshipMessage> unreadMessages = messageRepository
                .findByConversationIdAndIsReadFalse(conversationId);
        
        unreadMessages.stream()
                .filter(m -> !m.getSender().getId().equals(userId))
                .forEach(m -> m.setIsRead(true));
        
        messageRepository.saveAll(unreadMessages);
    }

    private MentorshipConversationDto convertToDto(MentorshipConversation conversation) {
        MentorshipConversationDto dto = new MentorshipConversationDto();
        dto.setId(conversation.getId());
        dto.setMentorId(conversation.getMentor().getId());
        dto.setMentorName(conversation.getMentor().getFirstName() + " " + conversation.getMentor().getLastName());
        dto.setMenteeId(conversation.getMentee().getId());
        dto.setMenteeName(conversation.getMentee().getFirstName() + " " + conversation.getMentee().getLastName());
        dto.setClubId(conversation.getClub().getId());
        dto.setSkillName(conversation.getSkillName());
        dto.setMentorLevel(conversation.getMentorLevel());
        dto.setMenteeLevel(conversation.getMenteeLevel());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setActive(conversation.getActive());
        
        List<MentorshipMessageDto> messageDtos = conversation.getMessages().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        dto.setMessages(messageDtos);
        
        return dto;
    }

    private MentorshipMessageDto convertToDto(MentorshipMessage message) {
        MentorshipMessageDto dto = new MentorshipMessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFirstName() + " " + message.getSender().getLastName());
        dto.setContent(message.getContent());
        dto.setSentAt(message.getSentAt());
        dto.setIsRead(message.getIsRead());
        return dto;
    }

    // Session Request Methods
    @Transactional
    public MentorshipSessionRequestDto createSessionRequest(Long conversationId, Long requesterId, 
            Long requestedUserId, LocalDateTime proposedDateTime, String description) {
        
        MentorshipConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        User requestedUser = userRepository.findById(requestedUserId)
                .orElseThrow(() -> new RuntimeException("Requested user not found"));

        // Check if there's already a pending request
        Optional<MentorshipSessionRequest> existing = sessionRequestRepository
                .findByConversationIdAndStatus(conversationId, MentorshipSessionRequest.RequestStatus.PENDING);
        
        if (existing.isPresent()) {
            throw new RuntimeException("A pending session request already exists for this conversation");
        }

        MentorshipSessionRequest request = new MentorshipSessionRequest();
        request.setConversation(conversation);
        request.setRequester(requester);
        request.setRequestedUser(requestedUser);
        request.setProposedDateTime(proposedDateTime);
        request.setDescription(description);
        request.setStatus(MentorshipSessionRequest.RequestStatus.PENDING);
        
        MentorshipSessionRequest saved = sessionRequestRepository.save(request);
        return convertToDto(saved);
    }

    @Transactional
    public MentorshipSessionRequestDto respondToSessionRequest(Long requestId, Long userId, 
            boolean accepted, String responseMessage) {
        
        MentorshipSessionRequest request = sessionRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (!request.getRequestedUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to respond to this request");
        }
        
        if (request.getStatus() != MentorshipSessionRequest.RequestStatus.PENDING) {
            throw new RuntimeException("This request has already been responded to");
        }

        request.setStatus(accepted ? MentorshipSessionRequest.RequestStatus.ACCEPTED : MentorshipSessionRequest.RequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        request.setResponseMessage(responseMessage);
        
        MentorshipSessionRequest saved = sessionRequestRepository.save(request);
        
        // Send WebSocket notification to the requester
        webSocketHandler.notifyUser(request.getRequester().getId(), Map.of(
            "type", "session_request_response",
            "requestId", requestId,
            "conversationId", request.getConversation().getId(),
            "accepted", accepted,
            "responseMessage", responseMessage != null ? responseMessage : "",
            "respondedAt", request.getRespondedAt().toString()
        ));
        
        return convertToDto(saved);
    }

    public List<MentorshipSessionRequestDto> getPendingRequestsForUser(Long userId) {
        List<MentorshipSessionRequest> requests = sessionRequestRepository
                .findByRequestedUserIdAndStatus(userId, MentorshipSessionRequest.RequestStatus.PENDING);
        return requests.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<MentorshipSessionRequestDto> getUserRequests(Long userId) {
        List<MentorshipSessionRequest> requests = sessionRequestRepository.findByRequesterId(userId);
        return requests.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private MentorshipSessionRequestDto convertToDto(MentorshipSessionRequest request) {
        MentorshipSessionRequestDto dto = new MentorshipSessionRequestDto();
        dto.setId(request.getId());
        dto.setConversationId(request.getConversation().getId());
        dto.setRequesterId(request.getRequester().getId());
        dto.setRequesterName(request.getRequester().getFirstName() + " " + request.getRequester().getLastName());
        dto.setRequestedUserId(request.getRequestedUser().getId());
        dto.setRequestedUserName(request.getRequestedUser().getFirstName() + " " + request.getRequestedUser().getLastName());
        dto.setProposedDateTime(request.getProposedDateTime());
        dto.setDescription(request.getDescription());
        dto.setStatus(request.getStatus().name());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setRespondedAt(request.getRespondedAt());
        dto.setResponseMessage(request.getResponseMessage());
        return dto;
    }

    // Feedback Methods
    @Transactional
    public MentorshipFeedbackDto submitFeedback(Long conversationId, Long giverId, Long receiverId, 
            Integer rating, String comment) {
        
        MentorshipConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        User giver = userRepository.findById(giverId)
                .orElseThrow(() -> new RuntimeException("Giver not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // Check if feedback already exists
        Optional<MentorshipFeedback> existing = feedbackRepository
                .findByConversationIdAndGiverId(conversationId, giverId);
        
        if (existing.isPresent()) {
            throw new RuntimeException("You have already submitted feedback for this mentorship");
        }

        MentorshipFeedback feedback = new MentorshipFeedback();
        feedback.setConversation(conversation);
        feedback.setGiver(giver);
        feedback.setReceiver(receiver);
        feedback.setRating(rating);
        feedback.setComment(comment);
        
        MentorshipFeedback saved = feedbackRepository.save(feedback);
        return convertToDto(saved);
    }

    public List<MentorshipFeedbackDto> getConversationFeedback(Long conversationId) {
        List<MentorshipFeedback> feedbacks = feedbackRepository.findByConversationId(conversationId);
        return feedbacks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<MentorshipFeedbackDto> getUserReceivedFeedback(Long userId) {
        List<MentorshipFeedback> feedbacks = feedbackRepository.findByReceiverId(userId);
        return feedbacks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private MentorshipFeedbackDto convertToDto(MentorshipFeedback feedback) {
        MentorshipFeedbackDto dto = new MentorshipFeedbackDto();
        dto.setId(feedback.getId());
        dto.setConversationId(feedback.getConversation().getId());
        dto.setGiverId(feedback.getGiver().getId());
        dto.setGiverName(feedback.getGiver().getFirstName() + " " + feedback.getGiver().getLastName());
        dto.setReceiverId(feedback.getReceiver().getId());
        dto.setReceiverName(feedback.getReceiver().getFirstName() + " " + feedback.getReceiver().getLastName());
        dto.setRating(feedback.getRating());
        dto.setComment(feedback.getComment());
        dto.setCreatedAt(feedback.getCreatedAt());
        return dto;
    }

    // Goal Methods
    @Transactional
    public MentorshipGoalDto createGoal(Long conversationId, Long menteeId, String title, 
            String description, LocalDateTime targetDate) {
        
        MentorshipConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        User mentee = userRepository.findById(menteeId)
                .orElseThrow(() -> new RuntimeException("Mentee not found"));

        MentorshipGoal goal = new MentorshipGoal();
        goal.setConversation(conversation);
        goal.setMentee(mentee);
        goal.setTitle(title);
        goal.setDescription(description);
        goal.setTargetDate(targetDate);
        
        MentorshipGoal saved = goalRepository.save(goal);
        return convertToDto(saved);
    }

    @Transactional
    public MentorshipGoalDto updateGoalProgress(Long goalId, Integer progress) {
        MentorshipGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        
        goal.setProgress(progress);
        
        if (progress >= 100) {
            goal.setStatus(MentorshipGoal.GoalStatus.COMPLETED);
            goal.setCompletedAt(LocalDateTime.now());
        }
        
        MentorshipGoal saved = goalRepository.save(goal);
        return convertToDto(saved);
    }

    public List<MentorshipGoalDto> getConversationGoals(Long conversationId) {
        List<MentorshipGoal> goals = goalRepository.findByConversationId(conversationId);
        return goals.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<MentorshipGoalDto> getMenteeGoals(Long menteeId) {
        List<MentorshipGoal> goals = goalRepository.findByMenteeId(menteeId);
        return goals.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public boolean areAllGoalsCompleted(Long conversationId) {
        List<MentorshipGoal> goals = goalRepository.findByConversationId(conversationId);
        if (goals.isEmpty()) return false;
        return goals.stream().allMatch(g -> g.getStatus() == MentorshipGoal.GoalStatus.COMPLETED);
    }

    private MentorshipGoalDto convertToDto(MentorshipGoal goal) {
        MentorshipGoalDto dto = new MentorshipGoalDto();
        dto.setId(goal.getId());
        dto.setConversationId(goal.getConversation().getId());
        dto.setMenteeId(goal.getMentee().getId());
        dto.setMenteeName(goal.getMentee().getFirstName() + " " + goal.getMentee().getLastName());
        dto.setTitle(goal.getTitle());
        dto.setDescription(goal.getDescription());
        dto.setStatus(goal.getStatus().name());
        dto.setProgress(goal.getProgress());
        dto.setTargetDate(goal.getTargetDate());
        dto.setCreatedAt(goal.getCreatedAt());
        dto.setCompletedAt(goal.getCompletedAt());
        return dto;
    }

    // Certificate Methods
    @Transactional
    public MentorshipCertificateDto generateCertificate(Long conversationId) {
        MentorshipConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Check if certificate already exists - return it instead of throwing error
        Optional<MentorshipCertificate> existing = certificateRepository.findByConversationId(conversationId);
        if (existing.isPresent()) {
            return convertToDto(existing.get());
        }

        // Check if all goals are completed
        if (!areAllGoalsCompleted(conversationId)) {
            throw new RuntimeException("All goals must be completed before generating a certificate");
        }

        MentorshipCertificate certificate = new MentorshipCertificate();
        certificate.setConversation(conversation);
        certificate.setMentee(conversation.getMentee());
        certificate.setMentor(conversation.getMentor());
        certificate.setSkillName(conversation.getSkillName());
        certificate.setDescription("Certificate of completion for mentorship in " + conversation.getSkillName());
        
        MentorshipCertificate saved = certificateRepository.save(certificate);
        
        // Generate PDF and store URL
        try {
            byte[] pdfBytes = certificatePdfService.generateCertificatePdf(saved);
            // For now, we'll return the DTO without URL - the PDF will be generated on demand
        } catch (Exception e) {
            System.err.println("Error generating PDF: " + e.getMessage());
        }
        
        return convertToDto(saved);
    }

    public byte[] generateCertificatePdf(Long conversationId) throws Exception {
        MentorshipCertificate certificate = certificateRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));
        return certificatePdfService.generateCertificatePdf(certificate);
    }

    public MentorshipCertificateDto getCertificateByConversation(Long conversationId) {
        Optional<MentorshipCertificate> certificate = certificateRepository.findByConversationId(conversationId);
        return certificate.map(this::convertToDto).orElse(null);
    }

    public List<MentorshipCertificateDto> getMenteeCertificates(Long menteeId) {
        List<MentorshipCertificate> certificates = certificateRepository.findByMenteeId(menteeId);
        return certificates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<MentorshipCertificateDto> getMentorCertificates(Long mentorId) {
        List<MentorshipCertificate> certificates = certificateRepository.findByMentorId(mentorId);
        return certificates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private MentorshipCertificateDto convertToDto(MentorshipCertificate certificate) {
        MentorshipCertificateDto dto = new MentorshipCertificateDto();
        dto.setId(certificate.getId());
        dto.setConversationId(certificate.getConversation().getId());
        dto.setMenteeId(certificate.getMentee().getId());
        dto.setMenteeName(certificate.getMentee().getFirstName() + " " + certificate.getMentee().getLastName());
        dto.setMentorId(certificate.getMentor().getId());
        dto.setMentorName(certificate.getMentor().getFirstName() + " " + certificate.getMentor().getLastName());
        dto.setSkillName(certificate.getSkillName());
        dto.setDescription(certificate.getDescription());
        dto.setIssuedAt(certificate.getIssuedAt());
        dto.setCertificateUrl(certificate.getCertificateUrl());
        return dto;
    }
}
