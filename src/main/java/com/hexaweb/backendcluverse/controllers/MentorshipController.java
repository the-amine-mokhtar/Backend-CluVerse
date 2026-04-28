package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.mentorship.MentorshipConversationDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipMessageDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipSessionRequestDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipFeedbackDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipGoalDto;
import com.hexaweb.backendcluverse.dto.mentorship.MentorshipCertificateDto;
import com.hexaweb.backendcluverse.services.MentorshipService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/mentorship")
public class MentorshipController {

    private final MentorshipService mentorshipService;

    public MentorshipController(MentorshipService mentorshipService) {
        this.mentorshipService = mentorshipService;
    }

    @PostMapping("/conversations")
    public ResponseEntity<MentorshipConversationDto> createConversation(
            @RequestParam Long mentorId,
            @RequestParam Long menteeId,
            @RequestParam Long clubId,
            @RequestParam String skillName,
            @RequestParam Integer mentorLevel,
            @RequestParam Integer menteeLevel) {
        
        MentorshipConversationDto conversation = mentorshipService.createConversation(
                mentorId, menteeId, clubId, skillName, mentorLevel, menteeLevel);
        return ResponseEntity.ok(conversation);
    }

    @GetMapping("/conversations/user/{userId}")
    public ResponseEntity<List<MentorshipConversationDto>> getUserConversations(
            @PathVariable Long userId) {
        List<MentorshipConversationDto> conversations = mentorshipService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MentorshipMessageDto>> getConversationMessages(
            @PathVariable Long conversationId) {
        List<MentorshipMessageDto> messages = mentorshipService.getConversationMessages(conversationId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MentorshipMessageDto> sendMessage(
            @PathVariable Long conversationId,
            @RequestParam Long senderId,
            @RequestBody String content) {
        
        MentorshipMessageDto message = mentorshipService.sendMessage(conversationId, senderId, content);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        mentorshipService.markMessagesAsRead(conversationId, userId);
        return ResponseEntity.ok().build();
    }

    // Session Request Endpoints
    @PostMapping("/session-requests")
    public ResponseEntity<MentorshipSessionRequestDto> createSessionRequest(
            @RequestParam Long conversationId,
            @RequestParam Long requesterId,
            @RequestParam Long requestedUserId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime proposedDateTime,
            @RequestParam String description) {
        MentorshipSessionRequestDto request = mentorshipService.createSessionRequest(
                conversationId, requesterId, requestedUserId, proposedDateTime, description);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/session-requests/{requestId}/respond")
    public ResponseEntity<MentorshipSessionRequestDto> respondToSessionRequest(
            @PathVariable Long requestId,
            @RequestParam Long userId,
            @RequestParam boolean accepted,
            @RequestParam(required = false) String responseMessage) {
        MentorshipSessionRequestDto request = mentorshipService.respondToSessionRequest(
                requestId, userId, accepted, responseMessage);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/session-requests/pending/{userId}")
    public ResponseEntity<List<MentorshipSessionRequestDto>> getPendingRequests(
            @PathVariable Long userId) {
        List<MentorshipSessionRequestDto> requests = mentorshipService.getPendingRequestsForUser(userId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/session-requests/user/{userId}")
    public ResponseEntity<List<MentorshipSessionRequestDto>> getUserRequests(
            @PathVariable Long userId) {
        List<MentorshipSessionRequestDto> requests = mentorshipService.getUserRequests(userId);
        return ResponseEntity.ok(requests);
    }

    // Feedback Endpoints
    @PostMapping("/feedback")
    public ResponseEntity<MentorshipFeedbackDto> submitFeedback(
            @RequestParam Long conversationId,
            @RequestParam Long giverId,
            @RequestParam Long receiverId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment) {
        MentorshipFeedbackDto feedback = mentorshipService.submitFeedback(
                conversationId, giverId, receiverId, rating, comment);
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/feedback/conversation/{conversationId}")
    public ResponseEntity<List<MentorshipFeedbackDto>> getConversationFeedback(
            @PathVariable Long conversationId) {
        List<MentorshipFeedbackDto> feedbacks = mentorshipService.getConversationFeedback(conversationId);
        return ResponseEntity.ok(feedbacks);
    }

    @GetMapping("/feedback/received/{userId}")
    public ResponseEntity<List<MentorshipFeedbackDto>> getUserReceivedFeedback(
            @PathVariable Long userId) {
        List<MentorshipFeedbackDto> feedbacks = mentorshipService.getUserReceivedFeedback(userId);
        return ResponseEntity.ok(feedbacks);
    }

    // Goal Endpoints
    @PostMapping("/goals")
    public ResponseEntity<MentorshipGoalDto> createGoal(
            @RequestParam Long conversationId,
            @RequestParam Long menteeId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime targetDate) {
        MentorshipGoalDto goal = mentorshipService.createGoal(conversationId, menteeId, title, description, targetDate);
        return ResponseEntity.ok(goal);
    }

    @PutMapping("/goals/{goalId}/progress")
    public ResponseEntity<MentorshipGoalDto> updateGoalProgress(
            @PathVariable Long goalId,
            @RequestParam Integer progress) {
        MentorshipGoalDto goal = mentorshipService.updateGoalProgress(goalId, progress);
        return ResponseEntity.ok(goal);
    }

    @GetMapping("/goals/conversation/{conversationId}")
    public ResponseEntity<List<MentorshipGoalDto>> getConversationGoals(
            @PathVariable Long conversationId) {
        List<MentorshipGoalDto> goals = mentorshipService.getConversationGoals(conversationId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/goals/mentee/{menteeId}")
    public ResponseEntity<List<MentorshipGoalDto>> getMenteeGoals(
            @PathVariable Long menteeId) {
        List<MentorshipGoalDto> goals = mentorshipService.getMenteeGoals(menteeId);
        return ResponseEntity.ok(goals);
    }

    @GetMapping("/goals/{conversationId}/completed")
    public ResponseEntity<Boolean> areAllGoalsCompleted(
            @PathVariable Long conversationId) {
        boolean completed = mentorshipService.areAllGoalsCompleted(conversationId);
        return ResponseEntity.ok(completed);
    }

    // Certificate Endpoints
    @PostMapping("/certificates/generate")
    public ResponseEntity<MentorshipCertificateDto> generateCertificate(
            @RequestParam Long conversationId) {
        MentorshipCertificateDto certificate = mentorshipService.generateCertificate(conversationId);
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/certificates/conversation/{conversationId}")
    public ResponseEntity<MentorshipCertificateDto> getCertificateByConversation(
            @PathVariable Long conversationId) {
        MentorshipCertificateDto certificate = mentorshipService.getCertificateByConversation(conversationId);
        if (certificate == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(certificate);
    }

    @GetMapping("/certificates/mentee/{menteeId}")
    public ResponseEntity<List<MentorshipCertificateDto>> getMenteeCertificates(
            @PathVariable Long menteeId) {
        List<MentorshipCertificateDto> certificates = mentorshipService.getMenteeCertificates(menteeId);
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/certificates/mentor/{mentorId}")
    public ResponseEntity<List<MentorshipCertificateDto>> getMentorCertificates(
            @PathVariable Long mentorId) {
        List<MentorshipCertificateDto> certificates = mentorshipService.getMentorCertificates(mentorId);
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/certificates/{conversationId}/pdf")
    public ResponseEntity<byte[]> downloadCertificatePdf(
            @PathVariable Long conversationId) {
        try {
            byte[] pdfBytes = mentorshipService.generateCertificatePdf(conversationId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "certificate_" + conversationId + ".pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
