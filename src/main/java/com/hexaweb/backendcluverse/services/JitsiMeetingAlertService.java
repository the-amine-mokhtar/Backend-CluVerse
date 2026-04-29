package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.enumerations.EventType;
import com.hexaweb.backendcluverse.enumerations.ParticipationStatus;
import com.hexaweb.backendcluverse.repositories.EventParticipantRepository;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JitsiMeetingAlertService {

    private static final Logger log = LoggerFactory.getLogger(JitsiMeetingAlertService.class);
    private static final int ALERT_WINDOW_MINUTES = 5; // Envoyer alerte entre -65 et -55 minutes
    private static final int ALERT_BEFORE_MINUTES = 60; // 1 heure avant

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final SmsService smsService;

    public JitsiMeetingAlertService(EventRepository eventRepository,
                                     EventParticipantRepository eventParticipantRepository,
                                     SmsService smsService) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.smsService = smsService;
    }

    /**
     * ✅ Scheduler: Envoie une alerte SMS aux participants 1 heure avant la réunion Jitsi
     * Exécution: toutes les minutes (60 secondes)
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void sendJitsiMeetingAlerts() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime alertStart = now.plusMinutes(ALERT_BEFORE_MINUTES - ALERT_WINDOW_MINUTES); // -65 min
            LocalDateTime alertEnd = now.plusMinutes(ALERT_BEFORE_MINUTES + ALERT_WINDOW_MINUTES);   // -55 min

            log.debug("[Jitsi Alert] Scanning for ONLINE events starting between {} and {}",
                    alertStart, alertEnd);

            List<Event> onlineEvents = eventRepository.findOnlineEventsStartingBetween(alertStart, alertEnd);

            if (onlineEvents.isEmpty()) {
                log.debug("[Jitsi Alert] No ONLINE events in alert window");
                return;
            }

            log.info("[Jitsi Alert] Found {} ONLINE event(s) in alert window", onlineEvents.size());

            for (Event event : onlineEvents) {
                sendAlertsForEvent(event);
            }

        } catch (Exception e) {
            log.error("[Jitsi Alert] ❌ Error in scheduler", e);
        }
    }

    /**
     * Envoie les alertes SMS pour tous les participants d'un événement Jitsi
     */
    private void sendAlertsForEvent(Event event) {
        if (!event.isOnline() || event.getMeetingUrl() == null) {
            log.warn("[Jitsi Alert] Event {} is not properly configured as ONLINE", event.getId());
            return;
        }

        // Chercher tous les participants REGISTERED
        List<EventParticipant> participants = eventParticipantRepository
                .findByEventIdAndStatus(event.getId(), ParticipationStatus.REGISTERED);

        if (participants.isEmpty()) {
            log.debug("[Jitsi Alert] No registered participants for '{}'", event.getTitle());
            return;
        }

        log.info("[Jitsi Alert] Sending alerts to {} participant(s) for '{}'",
                participants.size(), event.getTitle());

        for (EventParticipant participant : participants) {
            sendAlertToParticipant(participant, event);
        }
    }

    /**
     * Envoie une alerte SMS à un participant avec le lien Jitsi
     */
    private void sendAlertToParticipant(EventParticipant participant, Event event) {
        if (participant.getMeetingAlertSent() != null && participant.getMeetingAlertSent()) {
            log.debug("[Jitsi Alert] Alert already sent for participant {}", participant.getId());
            return;
        }

        String phone = participant.getUser().getPhone();
        if (phone == null || phone.isBlank()) {
            log.warn("[Jitsi Alert] Participant {} has no phone number", participant.getId());
            return;
        }

        phone = normalizePhone(phone);
        if (phone == null) {
            log.warn("[Jitsi Alert] Participant {} has invalid phone format", participant.getId());
            return;
        }

        String userName = participant.getUser().getFirstName();
        if (userName == null || userName.isBlank()) {
            userName = "Participant";
        }

        String message = buildJitsiAlertMessage(userName, event);

        log.info("[Jitsi Alert] Sending SMS to {} for event '{}'", phone, event.getTitle());

        boolean sent = smsService.sendSms(phone, message);

        if (sent) {
            participant.setMeetingAlertSent(true);
            eventParticipantRepository.save(participant);
            log.info("[Jitsi Alert] ✅ Alert sent to participant {}", participant.getId());
        } else {
            log.error("[Jitsi Alert] ❌ SMS failed for participant {} — will retry later",
                    participant.getId());
        }
    }

    /**
     * Construit le message SMS avec le lien Jitsi
     */
    private String buildJitsiAlertMessage(String userName, Event event) {
        // Format: "Hi [Name], [Event] starts in 1 hour! Join here: [link]"
        return String.format(
                "🎥 Bonjour %s!\n\n" +
                "L'événement \"%s\" commence dans 1 heure!\n\n" +
                "Rejoignez la réunion Jitsi:\n" +
                "%s\n\n" +
                "À bientôt! 👋\n" +
                "Cluverse",
                userName,
                event.getTitle(),
                event.getMeetingUrl()
        );
    }

    /**
     * Normalise le numéro de téléphone au format E.164
     */
    private String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String cleaned = raw.replaceAll("[\\s\\-().]+", "");

        if (cleaned.startsWith("+")) {
            return cleaned.length() >= 8 ? cleaned : null;
        }
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
            return cleaned.length() >= 8 ? cleaned : null;
        }
        if (cleaned.length() == 8) {
            return "+216" + cleaned; // Tunisia default
        }
        if (cleaned.length() >= 10) {
            return "+" + cleaned;
        }
        return null;
    }
}
