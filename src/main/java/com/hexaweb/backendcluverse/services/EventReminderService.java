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

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkEvents() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.plusHours(24);

        log.info("[Reminder Scheduler] Running at {} - checking for events starting around {}", 
                now, target);

        List<Event> events = eventRepository.findEventsStartingBetween(
                target.minusMinutes(30),
                target.plusMinutes(30)
        );

        if (events.isEmpty()) {
            log.debug("[Reminder] No events in 24h window");
            return;
        }

        log.info("[Reminder] Found {} event(s) starting in ~24 hours", events.size());

        for (Event event : events) {

            List<EventParticipant> participants =
                    eventParticipantRepository.findParticipantsForReminder(
                            event.getId(),
                            ParticipationStatus.REGISTERED
                    );

            if (participants.isEmpty()) {
                log.debug("[Reminder] No participants for event {}", event.getTitle());
                continue;
            }

            log.info("[Reminder] Found {} participants for event '{}' who want reminders", 
                    participants.size(), event.getTitle());

            for (EventParticipant participant : participants) {

                var user = participant.getUser();

                String phone = (user != null) ? user.getPhone() : null;
                if (phone == null || phone.isBlank()) {
                    log.debug("[Reminder] Participant {} has no phone number", participant.getId());
                    continue;
                }

                String userName = (user != null)
                        ? (user.getFirstName() + " " + user.getLastName()).trim()
                        : "Participant";

                // ✅ BUILD MESSAGE
                String message = messageBuilder.buildEventReminder(
                        userName,
                        event.getTitle(),
                        event.getStartDate()
                );

                // 📩 SEND SMS
                log.info("[Reminder] Sending SMS reminder to {} for event '{}'", 
                        phone, event.getTitle());
                smsService.sendSms(phone, message);

                // ⚠️ IMPORTANT: éviter double envoi
                participant.setReminderSent(true);
                eventParticipantRepository.save(participant);

                log.info("[Reminder] SMS sent → participant {} for event '{}'",
                        participant.getId(), event.getTitle());
            }
        }
    }
}