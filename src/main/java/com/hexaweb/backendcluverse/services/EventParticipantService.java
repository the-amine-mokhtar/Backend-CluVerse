package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.EventParticipantRequest;
import com.hexaweb.backendcluverse.dto.WaitingListDto;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.*;
import com.hexaweb.backendcluverse.enumerations.*;
import com.hexaweb.backendcluverse.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            EventService eventService,
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Optional<EventParticipant> existing =
                participantRepository.findByEventIdAndUserId(eventId, userId);

        if (existing.isPresent() &&
                existing.get().getStatus() != ParticipationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered");
        }

        long activeCount = participantRepository.countByEventIdAndStatusNot(
                eventId, ParticipationStatus.CANCELLED);

        boolean hasCapacity = event.getCapacity() == null
                || event.getCapacity() <= 0
                || activeCount < event.getCapacity();

        // =========================
        // REGISTER DIRECT
        // =========================
        if (hasCapacity) {

            EventParticipant p = EventParticipant.builder()
                    .event(event)
                    .user(user)
                    .status(ParticipationStatus.REGISTERED)
                    .reservedSeats(1)
                    .comment(req.getComment())
                    .contactInfo(req.getContactInfo())
                    .wantsReminder(Boolean.TRUE.equals(req.getWantsReminder()))
                    .build();

            participantRepository.save(p);
            eventService.updateParticipantsCount(eventId);

            smsService.sendSms(user.getPhone(),
                    "✅ Inscription confirmée : " + event.getTitle());

            return "REGISTERED";
        }

        // =========================
        // WAITING LIST
        // =========================
        boolean alreadyWaiting =
                waitingListRepository.findByEventIdAndUserId(eventId, userId).isPresent();

        if (alreadyWaiting) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Already in waiting list");
        }

        EventWaitingList wl = EventWaitingList.builder()
                .event(event)
                .user(user)
                .status(WaitingStatus.PENDING)
                .joinedAt(LocalDateTime.now())
                .build();

        waitingListRepository.save(wl);

        smsService.sendSms(user.getPhone(),
                "⏳ Liste d’attente : " + event.getTitle());

        return "WAITING_LIST_ADDED";
    }

    // =========================================================
    // ADD PARTICIPANT (LEGACY)
    // =========================================================
    public EventParticipant addParticipant(EventParticipantRequest req) {

        Event event = eventRepository.findById(req.getEventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(user)
                .status(ParticipationStatus.REGISTERED)
                .reservedSeats(req.getReservedSeats() != null ? req.getReservedSeats() : 1)
                .comment(req.getComment())
                .contactInfo(req.getContactInfo())
                .wantsReminder(Boolean.TRUE.equals(req.getWantsReminder()))
                .build();

        EventParticipant saved = participantRepository.save(participant);
        eventService.updateParticipantsCount(event.getId());

        smsService.sendSms(user.getPhone(),
                "✅ Registration confirmed: " + event.getTitle());

        return saved;
    }

    // =========================================================
    // UPDATE
    // =========================================================
    public EventParticipant updateParticipant(Long id, EventParticipantRequest req) {
        EventParticipant p = findOrThrow(id);
        applyFields(p, req);
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
        participantRepository.delete(p);
        eventService.updateParticipantsCount(p.getEvent().getId());
    }

    // =========================================================
    // CANCEL + AUTO PROMOTION
    // =========================================================
    public void cancelParticipation(Long id) {

        EventParticipant p = findOrThrow(id);

        p.setStatus(ParticipationStatus.CANCELLED);
        participantRepository.save(p);

        eventService.updateParticipantsCount(p.getEvent().getId());

        promoteFromWaitingList(p.getEvent().getId());
    }

    // =========================================================
    // REACTIVATE
    // =========================================================
    public EventParticipant reactivateParticipation(Long id) {

        EventParticipant p = findOrThrow(id);

        if (p.getStatus() != ParticipationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        p.setStatus(ParticipationStatus.REGISTERED);
        p.setDeletedAt(null);

        EventParticipant saved = participantRepository.save(p);
        eventService.updateParticipantsCount(p.getEvent().getId());

        return saved;
    }

    // =========================================================
    // WAITING LIST JOIN (LEGACY)
    // =========================================================
    public String joinWaitingList(Long eventId, Long userId, boolean accept) {

        if (!accept) return "USER_REFUSED";

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean exists = waitingListRepository
                .findByEventIdAndUserId(eventId, userId)
                .isPresent();

        if (exists) return "ALREADY_IN_WAITING_LIST";

        EventWaitingList wl = new EventWaitingList();
        wl.setEvent(event);
        wl.setUser(user);
        wl.setStatus(WaitingStatus.PENDING);

        waitingListRepository.save(wl);

        smsService.sendSms(user.getPhone(),
                "⏳ Added to waiting list: " + event.getTitle());

        return "WAITING_LIST_ADDED";
    }

    // =========================================================
    // WAITING CONFIRM (LEGACY SMS LINK)
    // =========================================================
    public String confirmPromotion(Long waitingId) {

        EventWaitingList wl = waitingListRepository.findById(waitingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        EventParticipant p = EventParticipant.builder()
                .event(wl.getEvent())
                .user(wl.getUser())
                .status(ParticipationStatus.REGISTERED)
                .build();

        participantRepository.save(p);
        waitingListRepository.delete(wl);

        eventService.updateParticipantsCount(p.getEvent().getId());

        smsService.sendSms(wl.getUser().getPhone(),
                "🎉 Confirmed for event: " + wl.getEvent().getTitle());

        return "CONFIRMED";
    }

    // =========================================================
    // AUTO PROMOTION CORE
    // =========================================================
    public void promoteFromWaitingList(Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException());

        long activeCount = participantRepository.countByEventIdAndStatusNot(
                eventId, ParticipationStatus.CANCELLED);

        if (event.getCapacity() != null && activeCount >= event.getCapacity()) return;

        List<EventWaitingList> list =
                waitingListRepository.findByEventIdOrderByJoinedAtAsc(eventId);

        if (list.isEmpty()) return;

        EventWaitingList first = list.get(0);

        // 🔥 FIX: Keep status as PENDING instead of immediately registering
        // User must confirm via SMS link before becoming a full participant
        first.setStatus(WaitingStatus.PENDING);
        waitingListRepository.save(first);

        // Send confirmation SMS with link
        String confirmationMessage = "🎉 You've been promoted from the waiting list for " + event.getTitle() 
                + ". Confirm your spot: [link]";
        smsService.sendSms(first.getUser().getPhone(), confirmationMessage);

        log.info("[WaitingList] User {} promoted for event {}, awaiting confirmation", 
                first.getUser().getId(), eventId);
    }

    // =========================================================
    // GET WAITING LIST FOR USER
    // =========================================================
    public List<WaitingListDto> getWaitingListForUser(Long userId) {
        List<EventWaitingList> waitingList =
                waitingListRepository.findByUserIdOrderByJoinedAtDesc(userId);

        return waitingList.stream().map(wl -> {
            // Get position in queue for this event
            List<EventWaitingList> queueForEvent =
                    waitingListRepository.findByEventIdOrderByJoinedAtAsc(wl.getEvent().getId());
            int position = 0;
            for (int i = 0; i < queueForEvent.size(); i++) {
                if (queueForEvent.get(i).getId().equals(wl.getId())) {
                    position = i + 1;  // 1-based index
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void applyFields(EventParticipant p, EventParticipantRequest req) {
        if (req.getComment() != null) p.setComment(req.getComment());
        if (req.getContactInfo() != null) p.setContactInfo(req.getContactInfo());
        if (req.getReservedSeats() != null) p.setReservedSeats(req.getReservedSeats());
        if (req.getWantsReminder() != null) p.setWantsReminder(req.getWantsReminder());
    }
}