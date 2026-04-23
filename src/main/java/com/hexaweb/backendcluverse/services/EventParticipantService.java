package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.EventParticipantRequest;
import com.hexaweb.backendcluverse.dto.WaitingListDto;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.*;
import com.hexaweb.backendcluverse.enumerations.*;
import com.hexaweb.backendcluverse.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventParticipantService extends EntityServiceImpl<EventParticipant, Long> {

    private static final Logger log = LoggerFactory.getLogger(EventParticipantService.class);

    private final EventParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final SmsService smsService;
    private final EventWaitingListRepository waitingListRepository;

    public EventParticipantService(
            EventParticipantRepository repository,
            EventRepository eventRepository,
            UserRepository userRepository,
            @Lazy EventService eventService,
            SmsService smsService,
            EventWaitingListRepository waitingListRepository
    ) {
        super(repository);
        this.participantRepository = repository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.smsService = smsService;
        this.waitingListRepository = waitingListRepository;
    }

    // =========================================================
    // FIND METHODS
    // =========================================================
    public List<EventParticipant> findByUserId(Long userId) {
        return participantRepository.findByUserIdWithEvent(userId);
    }

    public List<EventParticipant> findByEventId(Long eventId) {
        return participantRepository.findByEventId(eventId);
    }

    public List<EventParticipant> findCancelledByUserId(Long userId) {
        return participantRepository.findByUserIdWithEvent(userId)
                .stream()
                .filter(p -> p.getStatus() == ParticipationStatus.CANCELLED)
                .toList();
    }

    // =========================================================
    // OWNERSHIP CHECK
    // =========================================================
    public void checkOwnership(Long participationId, Long userId) {
        EventParticipant p = findOrThrow(participationId);
        if (!p.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only modify your own participations");
        }
    }

    // =========================================================
    // MAIN PARTICIPATION FLOW
    // =========================================================
    public String participate(Long eventId, Long userId, EventParticipantRequest req) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // ✅ FIX : sauvegarder le téléphone EN PREMIER
        // avant toute création de participation ou envoi SMS
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            user.setPhone(req.getPhone().trim());
            userRepository.save(user);
            log.info("[SMS] Téléphone mis à jour pour user {} : {}", userId, req.getPhone().trim());
        }

        Optional<EventParticipant> existing =
                participantRepository.findByEventIdAndUserId(eventId, userId);

        if (existing.isPresent() &&
                existing.get().getStatus() != ParticipationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered");
        }

        boolean alreadyWaiting =
                waitingListRepository.findByEventIdAndUserId(eventId, userId).isPresent();

        long activeCount = participantRepository.countByEventIdAndStatusNot(
                eventId, ParticipationStatus.CANCELLED);

        boolean hasCapacity = event.getCapacity() == null
                || event.getCapacity() <= 0
                || activeCount < event.getCapacity();

        // =========================
        // REGISTER DIRECT
        // =========================
        if (hasCapacity) {
            EventParticipant p;

            if (existing.isPresent()) {
                p = existing.get();
                p.setStatus(ParticipationStatus.REGISTERED);
                p.setDeletedAt(null);
                p.setComment(req.getComment());
                p.setContactInfo(req.getContactInfo());
                p.setWantsReminder(Boolean.TRUE.equals(req.getWantsReminder()));
                p.setReminderSent(false);
            } else {
                p = EventParticipant.builder()
                        .event(event)
                        .user(user)
                        .status(ParticipationStatus.REGISTERED)
                        .reservedSeats(req.getReservedSeats() != null ? req.getReservedSeats() : 1)
                        .comment(req.getComment())
                        .contactInfo(req.getContactInfo())
                        .wantsReminder(Boolean.TRUE.equals(req.getWantsReminder()))
                        .reminderSent(false)
                        .build();
            }

            participantRepository.save(p);
            eventService.updateParticipantsCount(eventId);

            // ✅ user.getPhone() est maintenant à jour
            String normalizedPhone = normalizePhone(user.getPhone());
            if (normalizedPhone != null) {
                boolean sent = smsService.sendSms(normalizedPhone,
                        "✅ Inscription confirmée : " + event.getTitle());
                if (!sent) {
                    log.error("[SMS] ❌ Échec SMS confirmation — user {}", userId);
                }
            } else {
                log.warn("[SMS] Numéro invalide pour user {} — SMS ignoré", userId);
            }

            return "REGISTERED";
        }

        // =========================
        // WAITING LIST
        // =========================
        if (alreadyWaiting) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already in waiting list");
        }

        EventWaitingList wl = EventWaitingList.builder()
                .event(event)
                .user(user)
                .status(WaitingStatus.PENDING)
                .joinedAt(LocalDateTime.now())
                .build();

        waitingListRepository.save(wl);

        // ✅ user.getPhone() est maintenant à jour
        String normalizedPhone = normalizePhone(user.getPhone());
        if (normalizedPhone != null) {
            boolean sent = smsService.sendSms(normalizedPhone,
                    "⏳ Liste d'attente : " + event.getTitle());
            if (!sent) {
                log.error("[SMS] ❌ Échec SMS liste d'attente — user {}", userId);
            }
        } else {
            log.warn("[SMS] Numéro invalide pour user {} — SMS ignoré", userId);
        }

        return "WAITING_LIST_ADDED";
    }

    // =========================================================
    // ADD PARTICIPANT (LEGACY)
    // =========================================================
    public EventParticipant addParticipant(EventParticipantRequest req) {

        Event event = eventRepository.findById(req.getEventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // ✅ FIX : sauvegarder le téléphone
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            user.setPhone(req.getPhone().trim());
            userRepository.save(user);
        }

        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(user)
                .status(ParticipationStatus.REGISTERED)
                .reservedSeats(req.getReservedSeats() != null ? req.getReservedSeats() : 1)
                .comment(req.getComment())
                .contactInfo(req.getContactInfo())
                .wantsReminder(Boolean.TRUE.equals(req.getWantsReminder()))
                .reminderSent(false)
                .build();

        EventParticipant saved = participantRepository.save(participant);
        eventService.updateParticipantsCount(event.getId());

        String normalizedPhone = normalizePhone(user.getPhone());
        if (normalizedPhone != null) {
            smsService.sendSms(normalizedPhone, "✅ Inscription confirmée : " + event.getTitle());
        }

        return saved;
    }

    // =========================================================
    // UPDATE
    // =========================================================
    public EventParticipant updateParticipant(Long id, EventParticipantRequest req) {
        EventParticipant p = findOrThrow(id);
        applyFields(p, req);

        // ✅ FIX : mettre à jour le téléphone lors d'un edit aussi
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            User user = p.getUser();
            user.setPhone(req.getPhone().trim());
            userRepository.save(user);
        }

        if (req.getStatus() != null) p.setStatus(req.getStatus());

        EventParticipant saved = participantRepository.save(p);
        eventService.updateParticipantsCount(p.getEvent().getId());
        return saved;
    }

    // =========================================================
    // DELETE
    // =========================================================
    public void deleteById(Long id) {
        EventParticipant p = findOrThrow(id);
        Long eventId = p.getEvent().getId();
        participantRepository.delete(p);
        eventService.updateParticipantsCount(eventId);
        eventService.promoteFromWaitingList(eventId);
    }

    // =========================================================
    // CANCEL + AUTO PROMOTION
    // =========================================================
    public void cancelParticipation(Long id) {
        EventParticipant p = findOrThrow(id);
        Long eventId = p.getEvent().getId();

        p.setStatus(ParticipationStatus.CANCELLED);
        participantRepository.save(p);

        eventService.updateParticipantsCount(eventId);
        eventService.promoteFromWaitingList(eventId);
    }

    // =========================================================
    // REACTIVATE
    // =========================================================
    public EventParticipant reactivateParticipation(Long id) {
        EventParticipant p = findOrThrow(id);

        if (p.getStatus() != ParticipationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only cancelled participations can be reactivated");
        }

        Long eventId = p.getEvent().getId();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        long activeCount = participantRepository.countByEventIdAndStatusNot(
                eventId, ParticipationStatus.CANCELLED);

        boolean hasCapacity = event.getCapacity() == null
                || event.getCapacity() <= 0
                || activeCount < event.getCapacity();

        if (!hasCapacity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Event is full. You can join the waiting list instead.");
        }

        p.setStatus(ParticipationStatus.REGISTERED);
        p.setDeletedAt(null);
        p.setReminderSent(false);

        EventParticipant saved = participantRepository.save(p);
        eventService.updateParticipantsCount(eventId);

        return saved;
    }

    // =========================================================
    // WAITING LIST JOIN (LEGACY)
    // =========================================================
    public String joinWaitingList(Long eventId, Long userId, boolean accept) {

        if (!accept) return "USER_REFUSED";

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean exists = waitingListRepository
                .findByEventIdAndUserId(eventId, userId)
                .isPresent();

        if (exists) return "ALREADY_IN_WAITING_LIST";

        EventWaitingList wl = new EventWaitingList();
        wl.setEvent(event);
        wl.setUser(user);
        wl.setStatus(WaitingStatus.PENDING);
        wl.setJoinedAt(LocalDateTime.now());

        waitingListRepository.save(wl);

        String normalizedPhone = normalizePhone(user.getPhone());
        if (normalizedPhone != null) {
            smsService.sendSms(normalizedPhone, "⏳ Liste d'attente : " + event.getTitle());
        }

        return "WAITING_LIST_ADDED";
    }

    // =========================================================
    // CONFIRM PROMOTION
    // =========================================================
    public String confirmPromotion(Long waitingId) {

        EventWaitingList wl = waitingListRepository.findById(waitingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Waiting list entry not found"));

        Long eventId = wl.getEvent().getId();

        long activeCount = participantRepository.countByEventIdAndStatusNot(
                eventId, ParticipationStatus.CANCELLED);
        Event event = wl.getEvent();

        boolean hasCapacity = event.getCapacity() == null
                || event.getCapacity() <= 0
                || activeCount < event.getCapacity();

        if (!hasCapacity) return "EVENT_FULL";

        EventParticipant p = EventParticipant.builder()
                .event(wl.getEvent())
                .user(wl.getUser())
                .status(ParticipationStatus.REGISTERED)
                .reservedSeats(1)
                .reminderSent(false)
                .build();

        participantRepository.save(p);
        waitingListRepository.delete(wl);
        eventService.updateParticipantsCount(eventId);

        String normalizedPhone = normalizePhone(wl.getUser().getPhone());
        if (normalizedPhone != null) {
            smsService.sendSms(normalizedPhone, "🎉 Confirmé pour l'événement : " + wl.getEvent().getTitle());
        }

        return "CONFIRMED";
    }

    // =========================================================
    // GET WAITING LIST FOR USER
    // =========================================================
    public List<WaitingListDto> getWaitingListForUser(Long userId) {
        List<EventWaitingList> waitingList =
                waitingListRepository.findByUserIdOrderByJoinedAtDesc(userId);

        return waitingList.stream().map(wl -> {
            List<EventWaitingList> queueForEvent =
                    waitingListRepository.findByEventIdOrderByJoinedAtAsc(wl.getEvent().getId());
            int position = 0;
            for (int i = 0; i < queueForEvent.size(); i++) {
                if (queueForEvent.get(i).getId().equals(wl.getId())) {
                    position = i + 1;
                    break;
                }
            }
            return WaitingListDto.fromEntity(wl, position);
        }).toList();
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private EventParticipant findOrThrow(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Participation not found"));
    }

    private void applyFields(EventParticipant p, EventParticipantRequest req) {
        if (req.getComment() != null) p.setComment(req.getComment());
        if (req.getContactInfo() != null) p.setContactInfo(req.getContactInfo());
        if (req.getReservedSeats() != null) p.setReservedSeats(req.getReservedSeats());
        if (req.getWantsReminder() != null) p.setWantsReminder(req.getWantsReminder());
    }

    private String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.replaceAll("[\\s\\-().]+", "");
        if (cleaned.startsWith("+"))  return cleaned.length() >= 8 ? cleaned : null;
        if (cleaned.startsWith("00")) { cleaned = "+" + cleaned.substring(2); return cleaned.length() >= 8 ? cleaned : null; }
        if (cleaned.length() == 8)    return "+216" + cleaned;
        if (cleaned.length() >= 10)   return "+" + cleaned;
        return null;
    }
}