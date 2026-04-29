package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.event.SmsNotification;
import com.hexaweb.backendcluverse.repositories.SmsNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SmsNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);

    private final SmsNotificationRepository smsNotificationRepository;
    // ✅ FIX : injecter le vrai SmsService (Twilio) au lieu du mock
    private final SmsService smsService;

    public SmsNotificationService(SmsNotificationRepository smsNotificationRepository,
                                  SmsService smsService) {
        this.smsNotificationRepository = smsNotificationRepository;
        this.smsService = smsService;
    }

    /**
     * Envoie un SMS de rappel et sauvegarde l'historique en base.
     */
    public SmsNotification sendReminderSms(Long participationId, Long eventId,
                                           String phoneNumber, String message) {
        try {
            // Ne pas envoyer si déjà envoyé pour cette participation
            if (smsNotificationRepository.existsByParticipationIdAndStatus(participationId, "SENT")) {
                logger.warn("[SMS] Déjà envoyé pour participation {}", participationId);
                return null;
            }

            SmsNotification sms = new SmsNotification();
            sms.setParticipationId(participationId);
            sms.setEventId(eventId);
            sms.setPhoneNumber(phoneNumber);
            sms.setMessage(message);
            sms.setStatus("PENDING");
            sms.setSentAt(LocalDateTime.now());

            // ✅ FIX : appel au vrai SmsService Twilio (plus de mock)
            boolean sent = smsService.sendSms(phoneNumber, message);

            if (sent) {
                sms.setStatus("SENT");
                sms.setSmsProviderReference("twilio-" + System.currentTimeMillis());
                logger.info("[SMS] ✅ Rappel envoyé à {} pour event {}", phoneNumber, eventId);
            } else {
                sms.setStatus("FAILED");
                sms.setErrorMessage("Échec d'envoi via Twilio");
                logger.error("[SMS] ❌ Échec envoi SMS à {}", phoneNumber);
            }

            return smsNotificationRepository.save(sms);

        } catch (Exception e) {
            logger.error("[SMS] Exception: {}", e.getMessage(), e);
            return null;
        }
    }

    public boolean hasReminderBeenSent(Long participationId) {
        return smsNotificationRepository.existsByParticipationIdAndStatus(participationId, "SENT");
    }

    public List<SmsNotification> getSmsHistory(Long participationId) {
        return smsNotificationRepository.findByParticipationId(participationId);
    }
}