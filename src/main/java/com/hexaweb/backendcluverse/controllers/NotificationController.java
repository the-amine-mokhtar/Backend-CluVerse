package com.hexaweb.backendcluverse.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.hexaweb.backendcluverse.entities.Notification;
import com.hexaweb.backendcluverse.repositories.NotificationRepository;
import com.hexaweb.backendcluverse.services.BudgetAlertEmailService;
import com.hexaweb.backendcluverse.utils.JwtUtil;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BudgetAlertEmailService budgetAlertEmailService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(@RequestParam Long clubId) {
        return ResponseEntity.ok(notificationRepository.findByClubIdOrderByCreatedAtDesc(clubId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok("Notification marked as read");
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestParam Long clubId) {
        notificationRepository.markAllAsReadByClubId(clubId);
        return ResponseEntity.ok("All notifications marked as read");
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestParam Long clubId) {
        return ResponseEntity.ok(notificationRepository.countByClubIdAndReadFalse(clubId));
    }

    @PostMapping("/budget-alert-email")
    public ResponseEntity<String> sendBudgetAlertEmail(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody BudgetAlertEmailRequest request
    ) {
        String token = jwtUtil.resolveBearerToken(authHeader);
        String recipientEmail = jwtUtil.extractEmail(token);
        String firstName = jwtUtil.extractFirstName(token);
        String lastName = jwtUtil.extractLastName(token);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to resolve current user email from token");
        }

        if (request.alerts() == null || request.alerts().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one alert is required");
        }

        String recipientName = (firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim());
        recipientName = recipientName.trim();
        if (recipientName.isBlank()) {
            recipientName = request.recipientName();
        }

        BudgetAlertEmailRequest resolvedRequest = new BudgetAlertEmailRequest(
                recipientEmail,
                recipientName,
                request.clubName(),
                request.exerciseYear(),
                request.alerts(),
                request.triggeredAt()
        );

        budgetAlertEmailService.sendBudgetAlertEmail(resolvedRequest);
        return ResponseEntity.ok("Budget alert email sent");
    }

    public record BudgetAlertEmailRequest(
            String recipientEmail,
            String recipientName,
            String clubName,
            Integer exerciseYear,
            List<BudgetAlertItem> alerts,
            String triggeredAt
    ) {}

    public record BudgetAlertItem(
            String title,
            String department,
            Integer utilization,
            Integer reachedThreshold,
            String level
    ) {}
}