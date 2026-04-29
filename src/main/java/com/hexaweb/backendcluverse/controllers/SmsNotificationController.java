package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.services.SmsNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * ✅ Contrôleur pour gérer les notifications SMS
 */
@RestController
@RequestMapping("/api/sms-notifications")
@CrossOrigin(origins = "http://localhost:4200")
public class SmsNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationController.class);

    @Autowired
    private SmsNotificationService smsNotificationService;

    /**
     * ✅ GET : Vérifier l'historique des SMS pour une participation
     * @param participationId ID de la participation
     * @return Liste des SMS envoyés
     */
    @GetMapping("/history/{participationId}")
    public ResponseEntity<?> getSmsHistory(@PathVariable Long participationId) {
        try {
            var history = smsNotificationService.getSmsHistory(participationId);
            return ResponseEntity.ok(Map.of(
                "participationId", participationId,
                "count", history.size(),
                "notifications", history
            ));
        } catch (Exception e) {
            logger.error("[SmsController] Error fetching SMS history: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ GET : Vérifier le statut des SMS (test endpoint)
     * @return Statut du système SMS
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSmsStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("service", "SMS Notification Service");
            status.put("status", "ACTIVE");
            status.put("scheduler", "Runs hourly to check events in ~24h");
            status.put("description", "Sends personalized SMS reminders to participants");
            status.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("[SmsController] Error getting status: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ POST : Envoyer un SMS de test (development only)
     * @param participationId ID de la participation
     * @param eventId ID de l'event
     * @param phone Numéro de téléphone
     * @return Résultat de l'envoi
     */
    @PostMapping("/send-test")
    public ResponseEntity<?> sendTestSms(
            @RequestParam Long participationId,
            @RequestParam Long eventId,
            @RequestParam String phone) {
        try {
            String testMessage = "📢 TEST SMS: This is a test reminder notification.";
            var sms = smsNotificationService.sendReminderSms(participationId, eventId, phone, testMessage);

            if (sms != null && "SENT".equals(sms.getStatus())) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "SMS sent successfully",
                    "id", sms.getId()
                ));
            } else {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "Failed to send SMS"
                ));
            }
        } catch (Exception e) {
            logger.error("[SmsController] Error sending test SMS: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
