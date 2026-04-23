package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.event.EventParticipant;
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
public class EventReminderService {

    private static final Logger log = LoggerFactory.getLogger(EventReminderService.class);

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventMessageBuilder messageBuilder;
    private final SmsService smsService;

    public EventReminderService(EventRepository eventRepository,
                                EventParticipantRepository eventParticipantRepository,
                                EventMessageBuilder messageBuilder,
                                SmsService smsService) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.messageBuilder = messageBuilder;
        this.smsService = smsService;
    }

    @Scheduled(fixedRate = 10000)

    @Transactional
    public void checkEvents() {

        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime target = now.plusHours(24);

        log.info("[Reminder] Scheduler running at {} — scanning 24h window [{} → {}]",
                now,
                target.minusHours(3),
                target.plusHours(3));

        List<Event> events = eventRepository.findEventsStartingBetween(
                target.minusHours(3),
                target.plusHours(3)
        );

        if (events.isEmpty()) {
            log.debug("[Reminder] No events in the 24h window");
            return;
        }

        log.info("[Reminder] {} event(s) found in window", events.size());

        for (Event event : events) {

            // ✅ FIX 1 : findParticipantsForReminder doit retourner les participants
            //    REGISTERED avec wantsReminder=true ET reminderSent=false.
            //    Vérifiez que votre requête JPQL/repo ressemble à :
            //    "WHERE p.event.id = :eventId
            //       AND p.status = :status          ← REGISTERED (pas "statusNot")
            //       AND p.wantsReminder = true
            //       AND p.reminderSent = false"
            List<EventParticipant> participants =
                    eventParticipantRepository.findParticipantsForReminder(
                            event.getId(),
                            ParticipationStatus.REGISTERED   // status INCLUS, pas exclu
                    );

            if (participants.isEmpty()) {
                log.debug("[Reminder] No pending-reminder participants for '{}'", event.getTitle());
                continue;
            }

            log.info("[Reminder] {} participant(s) to remind for '{}'",
                    participants.size(), event.getTitle());

            for (EventParticipant participant : participants) {

                var user  = participant.getUser();
                String phone = (user != null) ? user.getPhone() : null;

                if (phone == null || phone.isBlank()) {
                    log.warn("[Reminder] Participant {} has no phone — skipping", participant.getId());
                    continue;
                }

                // ✅ FIX 2 : Normalisation E.164 avant envoi
                phone = normalizePhone(phone);
                if (phone == null) {
                    log.warn("[Reminder] Participant {} has invalid phone format — skipping",
                            participant.getId());
                    continue;
                }

                String userName = (user.getFirstName() + " " + user.getLastName()).trim();
                if (userName.isBlank()) userName = "Participant";

                String message = messageBuilder.buildEventReminder(
                        userName,
                        event.getTitle(),
                        event.getStartDate()
                );

                log.info("[Reminder] Sending SMS to {} for '{}'", phone, event.getTitle());

                // ✅ FIX 3 : On utilise le boolean retourné pour retry ou confirmer
                boolean sent = smsService.sendSms(phone, message);

                if (sent) {
                    participant.setReminderSent(true);
                    eventParticipantRepository.save(participant);
                    log.info("[Reminder] ✅ SMS sent → participant {}", participant.getId());
                } else {
                    // reminderSent reste false → le scheduler retentera au prochain cycle
                    log.error("[Reminder] ❌ SMS failed → participant {} — will retry on next run",
                            participant.getId());
                }
            }
        }
    }

    /**
     * Normalise le numéro en E.164 pour Twilio.
     * +21612345678  → déjà valide
     * 21612345678   → +21612345678
     * 12345678      → +21612345678  (8 chiffres tunisiens)
     * 0021612345678 → +21612345678
     */
    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("[\\s\\-().]+", "");

        if (cleaned.startsWith("+")) {
            return cleaned.length() >= 8 ? cleaned : null;
        }
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
            return cleaned.length() >= 8 ? cleaned : null;
        }
        if (cleaned.length() == 8) {
            return "+216" + cleaned;
        }
        if (cleaned.length() >= 10) {
            return "+" + cleaned;
        }
        return null;
    }
}