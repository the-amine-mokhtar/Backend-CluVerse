package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.event.SmsNotification;
import com.hexaweb.backendcluverse.repositories.SmsNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * ✅ Service pour envoyer les notifications SMS
 * Peut utiliser Twilio, AWS SNS, ou un fournisseur SMS local
 */
@Service
public class SmsNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);

    @Autowired
    private SmsNotificationRepository smsNotificationRepository;

    /**
     * ✅ Envoyer un SMS de rappel pour un event
     * @param participationId ID de la participation
     * @param eventId ID de l'event
     * @param phoneNumber Numéro de téléphone du participant
     * @param message Message à envoyer
     * @return SmsNotification l'objet de notification créé
     */
    public SmsNotification sendReminderSms(Long participationId, Long eventId, String phoneNumber, String message) {
        try {
            // ✅ Étape 1 : Vérifier si un SMS a déjà été envoyé
            if (smsNotificationRepository.existsByParticipationIdAndStatus(participationId, "SENT")) {
                logger.warn("[SMS] Notification already sent for participation: {}", participationId);
                return null;
            }

            // ✅ Étape 2 : Créer l'objet SmsNotification
            SmsNotification sms = new SmsNotification();
            sms.setParticipationId(participationId);
            sms.setEventId(eventId);
            sms.setPhoneNumber(phoneNumber);
            sms.setMessage(message);
            sms.setStatus("PENDING");
            sms.setSentAt(LocalDateTime.now());

            // ✅ Étape 3 : INTÉGRATION AVEC FOURNISSEUR SMS (à implémenter)
            // Options : Twilio, AWS SNS, Vonage, etc.
            boolean sent = this.sendViaTwilio(phoneNumber, message);

            // ✅ Étape 4 : Mettre à jour le statut
            if (sent) {
                sms.setStatus("SENT");
                sms.setSmsProviderReference("twilio-" + System.currentTimeMillis());
                logger.info("[SMS] ✅ Reminder sent to {} for event {}", phoneNumber, eventId);
            } else {
                sms.setStatus("FAILED");
                sms.setErrorMessage("Failed to send via SMS provider");
                logger.error("[SMS] ❌ Failed to send SMS to {}", phoneNumber);
            }

            // ✅ Étape 5 : Sauvegarder en base de données
            return smsNotificationRepository.save(sms);

        } catch (Exception e) {
            logger.error("[SMS] Exception sending reminder: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * ✅ STUB : Envoyer via Twilio
     * À remplacer par l'intégration réelle Twilio
     * @param phoneNumber Numéro cible
     * @param message Message à envoyer
     * @return true si succès, false sinon
     */
    private boolean sendViaTwilio(String phoneNumber, String message) {
        try {
            // TODO: Intégration Twilio
            // TwilioRestClient client = Twilio.getRestClient();
            // Message msg = Message.creator(
            //     new PhoneNumber("+1234567890"),  // From number
            //     new PhoneNumber(phoneNumber),     // To number
            //     message
            // ).create();
            // return msg.getSid() != null;

            // ✅ POUR TESTS : Simuler l'envoi
            logger.debug("[Twilio Mock] Sending SMS to {} : {}", phoneNumber, message);
            return true;

        } catch (Exception e) {
            logger.error("[Twilio] Error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Vérifier si un SMS a déjà été envoyé
     */
    public boolean hasReminderBeenSent(Long participationId) {
        return smsNotificationRepository.existsByParticipationIdAndStatus(participationId, "SENT");
    }

    /**
     * ✅ Obtenir l'historique des SMS pour une participation
     */
    public java.util.List<SmsNotification> getSmsHistory(Long participationId) {
        return smsNotificationRepository.findByParticipationId(participationId);
    }
}
