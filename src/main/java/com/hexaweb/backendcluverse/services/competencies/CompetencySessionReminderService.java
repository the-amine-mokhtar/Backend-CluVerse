package com.hexaweb.backendcluverse.services.competencies;

import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.competencies.CompetencySession;
import com.hexaweb.backendcluverse.entities.competencies.CompetencySessionParticipant;
import com.hexaweb.backendcluverse.enumerations.SessionStatus;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencySessionParticipantRepository;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencySessionRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetencySessionReminderService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CompetencySessionRepository sessionRepository;
    private final CompetencySessionParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final TwilioSmsService twilioSmsService;

    @Value("${competency.reminder.enabled:true}")
    private boolean reminderEnabled;

    @Value("${competency.reminder.tolerance-minutes:3}")
    private long toleranceMinutes;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Scheduled(cron = "${competency.reminder.cron:0 */5 * * * *}")
    @Transactional
    public void sendScheduledReminders() {
        if (!reminderEnabled) {
            return;
        }

        int sent = 0;
        sent += dispatchJMinus1();
        sent += dispatchHMinus1();

        log.info("Competency reminder job executed: {} reminder(s) sent", sent);
    }

    private int dispatchJMinus1() {
        return dispatchForTarget(Duration.ofHours(24), true);
    }

    private int dispatchHMinus1() {
        return dispatchForTarget(Duration.ofHours(1), false);
    }

    private int dispatchForTarget(Duration beforeStart, boolean jMinus1) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.plus(beforeStart);
        LocalDateTime windowStart = target.minusMinutes(toleranceMinutes);
        LocalDateTime windowEnd = target.plusMinutes(toleranceMinutes);

        List<CompetencySession> sessions = sessionRepository.findByStatusAndStartsAtBetween(
                SessionStatus.SCHEDULED,
                windowStart,
                windowEnd
        );

        int sentCount = 0;
        for (CompetencySession session : sessions) {
            List<CompetencySessionParticipant> participants = participantRepository.findBySession_Id(session.getId());

            for (CompetencySessionParticipant participant : participants) {
                if (alreadySent(participant, jMinus1)) {
                    continue;
                }

                User user = userRepository.findById(participant.getUserId()).orElse(null);
                if (user == null) {
                    continue;
                }

                String payload = buildReminderMessage(jMinus1, session);
                twilioSmsService.sendSms(user.getPhone(), payload);

                if (jMinus1) {
                    participant.setJMinus1ReminderSentAt(LocalDateTime.now());
                } else {
                    participant.setHMinus1ReminderSentAt(LocalDateTime.now());
                }
                participantRepository.save(participant);
                sentCount++;
            }
        }

        return sentCount;
    }

    private boolean alreadySent(CompetencySessionParticipant participant, boolean jMinus1) {
        return jMinus1
                ? participant.getJMinus1ReminderSentAt() != null
                : participant.getHMinus1ReminderSentAt() != null;
    }

    private String buildReminderMessage(boolean jMinus1, CompetencySession session) {
        String prefix = jMinus1 ? "[RAPPEL J-1]" : "[RAPPEL H-1]";
        String when = session.getStartsAt() == null ? "date non définie" : session.getStartsAt().format(DATE_FORMAT);
        String meet = (session.getMeetLink() == null || session.getMeetLink().isBlank())
                ? "Lien Meet non disponible"
                : session.getMeetLink().trim();

        return prefix + " Session: " + session.getTitle() +
                " | Début: " + when +
                " | Meet: " + meet;
    }
}
