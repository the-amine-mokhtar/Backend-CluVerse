package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.EventParticipantRequest;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.enumerations.ParticipationStatus;
import com.hexaweb.backendcluverse.repositories.EventParticipantRepository;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
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
    private final EventService eventService;
    private final EventParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventParticipantService(EventParticipantRepository repository,
                                   EventRepository eventRepository,
                                   UserRepository userRepository,
                                   EventService eventService) {
        super(repository);
        this.participantRepository = repository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
    }
    public List<EventParticipant> findByUserId(Long userId) {
        List<EventParticipant> participants = participantRepository.findByUserIdWithEvent(userId);

        // ✅ Normalize any invalid status values in existing data
        for (EventParticipant p : participants) {
            if (p.getStatus() != null && "COMPLETED".equals(p.getStatus().name())) {
                p.setStatus(ParticipationStatus.ATTENDED);
                participantRepository.save(p); // Save the corrected status
            }
        }

        return participants;
    }
    public List<EventParticipant> findByEventId(Long eventId) {
        return participantRepository.findByEventId(eventId);
    }



    public void checkOwnership(Long participationId, Long userId) {
        EventParticipant p = participantRepository.findById(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation not found"));
        if (!p.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your participation");
        }
    }

    public EventParticipant addParticipant(EventParticipantRequest req) {
        Event event = eventRepository.findById(req.getEventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (req.getFullName() == null || req.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (req.getPhone() == null || req.getPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
        }

        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        applyUserContactUpdates(user, req, false);

        // ── Check if a participation already exists (any status) ──────────────
        Optional<EventParticipant> existing =
                participantRepository.findByEventIdAndUserId(req.getEventId(), req.getUserId());

        if (existing.isPresent()) {
            EventParticipant ep = existing.get();

            if (ep.getStatus() == ParticipationStatus.REGISTERED ||
                    ep.getStatus() == ParticipationStatus.ATTENDED) {
                // Already active — block
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered for this event");
            }

            // Status == CANCELLED → reactivate the existing row
            ep.setStatus(ParticipationStatus.REGISTERED);
            ep.setComment(req.getComment());
            ep.setContactInfo(req.getContactInfo());
            ep.setReservedSeats(req.getReservedSeats() != null ? req.getReservedSeats() : 1);
            ep.setWantsReminder(req.getWantsReminder() != null ? req.getWantsReminder() : false);
            ep.setDietaryRequirements(req.getDietaryRequirements());
            ep.setEmergencyContact(req.getEmergencyContact());
            ep.setTeamName(req.getTeamName());
            EventParticipant saved = participantRepository.save(ep);

// 🔥 Mettre à jour le compteur
            eventService.updateParticipantsCount(ep.getEvent().getId());

            return saved;        }

        // ── Capacity check (only for brand-new registrations) ─────────────────
        long activeCount = participantRepository.countByEventIdAndStatusNot(
                req.getEventId(), ParticipationStatus.CANCELLED);
        if (event.getCapacity() != null && activeCount >= event.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event is full");
        }

        // ── Create new participation ───────────────────────────────────────────
        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(user)
                .comment(req.getComment())
                .contactInfo(req.getContactInfo())
                .reservedSeats(req.getReservedSeats() != null ? req.getReservedSeats() : 1)
                .wantsReminder(req.getWantsReminder() != null ? req.getWantsReminder() : false)
                .status(ParticipationStatus.REGISTERED)
                .dietaryRequirements(req.getDietaryRequirements())
                .emergencyContact(req.getEmergencyContact())
                .teamName(req.getTeamName())
                .build();

        EventParticipant saved = participantRepository.save(participant);

// 🔥 Mettre à jour le compteur
        eventService.updateParticipantsCount(participant.getEvent().getId());

        return saved;    }

    public EventParticipant updateParticipant(Long id, EventParticipantRequest req) {
        EventParticipant participant = participantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation not found"));

        if (req.getComment() != null)             participant.setComment(req.getComment());
        if (req.getContactInfo() != null)         participant.setContactInfo(req.getContactInfo());
        if (req.getReservedSeats() != null)       participant.setReservedSeats(req.getReservedSeats());
        if (req.getWantsReminder() != null)       participant.setWantsReminder(req.getWantsReminder());
        if (req.getStatus() != null) {
            // ✅ Normalize invalid status values
            ParticipationStatus normalizedStatus = req.getStatus();
            if ("COMPLETED".equals(req.getStatus().name())) {
                normalizedStatus = ParticipationStatus.ATTENDED;
            }
            participant.setStatus(normalizedStatus);
        }
        if (req.getDietaryRequirements() != null) participant.setDietaryRequirements(req.getDietaryRequirements());
        if (req.getEmergencyContact() != null)    participant.setEmergencyContact(req.getEmergencyContact());
        if (req.getTeamName() != null)            participant.setTeamName(req.getTeamName());

        applyUserContactUpdates(participant.getUser(), req);

        return participantRepository.save(participant);
    }

    public void deleteById(Long id) {
        EventParticipant participant = participantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation not found"));

        participantRepository.delete(participant); // supprime réellement
        eventService.updateParticipantsCount(participant.getEvent().getId());
    }
    public List<EventParticipant> findCancelledByUserId(Long userId) {
        return participantRepository.findByUserIdWithEvent(userId).stream()
                .filter(p -> p.getStatus() == ParticipationStatus.CANCELLED)
                .toList();
    }
    public void cancelParticipation(Long id) {
        EventParticipant participant = participantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation not found"));

        participant.setStatus(ParticipationStatus.CANCELLED);
        participant.setDeletedAt(LocalDateTime.now());
        participantRepository.save(participant);

        eventService.updateParticipantsCount(participant.getEvent().getId());
    }

    private void applyUserContactUpdates(User user, EventParticipantRequest req) {
        applyUserContactUpdates(user, req, true);
    }

    private void applyUserContactUpdates(User user, EventParticipantRequest req, boolean strictEmailUpdate) {
        boolean updated = false;

        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            String[] parts = req.getFullName().trim().split("\\s+", 2);
            user.setFirstName(parts[0]);
            user.setLastName(parts.length > 1 ? parts[1] : "");
            updated = true;
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String newEmail = req.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(user.getEmail())) {
                Optional<User> existingEmail = userRepository.findFirstByEmailOrderByIdDesc(newEmail);
                if (existingEmail.isPresent() && !existingEmail.get().getId().equals(user.getId())) {
                    if (strictEmailUpdate) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
                    }
                } else {
                    user.setEmail(newEmail);
                    updated = true;
                }
            }
        }

        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            String newPhone = req.getPhone().trim();
            if (!newPhone.equals(user.getPhone())) {
                user.setPhone(newPhone);
                updated = true;
            }
        }

        if (updated) {
            userRepository.save(user);
        }
    }

}