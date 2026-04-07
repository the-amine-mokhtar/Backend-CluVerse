package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Notification;
import com.hexaweb.backendcluverse.repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(@RequestParam Long clubId) {
        return ResponseEntity.ok(notificationRepository.findByClubIdOrderByCreatedAtDesc(clubId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
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
}