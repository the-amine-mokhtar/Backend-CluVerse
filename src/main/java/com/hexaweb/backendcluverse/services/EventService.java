package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.EventRequest;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.event.*;
import com.hexaweb.backendcluverse.enumerations.*;
import com.hexaweb.backendcluverse.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class EventService extends EntityServiceImpl<Event, Long> {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final CampaignAccessRepository       campaignAccessRepository;
    private final EventRepository                eventRepository;
    private final ClubRepository                 clubRepository;
    private final CampaignRepository             campaignRepository;
    private final LocationRepository             locationRepository;
    private final EventParticipantRepository     eventParticipantRepository;
    private final CampaignService                campaignService;
    private final EventWaitingListRepository     waitingListRepository;
    private final UserRepository                 userRepository;
    private final SmsService                     smsService;

    public EventService(EventRepository repository,
                        ClubRepository clubRepository,
                        CampaignRepository campaignRepository,
                        EventParticipantRepository eventParticipantRepository,
                        LocationRepository locationRepository,
                        CampaignService campaignService,
                        SmsService smsService,
                        EventWaitingListRepository waitingListRepository,
                        UserRepository userRepository,
                        CampaignAccessRepository campaignAccessRepository) {
        super(repository);
        this.eventRepository            = repository;
        this.clubRepository             = clubRepository;
        this.campaignRepository         = campaignRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.locationRepository         = locationRepository;
        this.campaignService            = campaignService;
        this.smsService                 = smsService;
        this.waitingListRepository      = waitingListRepository;
        this.userRepository             = userRepository;
        this.campaignAccessRepository   = campaignAccessRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════

    public Event createEvent(EventRequest req, Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Club not found: " + clubId));

        Event event = new Event();
        mapRequest(event, req, false);
        event.setStatus(req.getStatus() != null ? req.getStatus() : EventStatus.PLANNED);
        event.setClub(club);

        validateEventMode(event, req);
        Location location = attachLocation(event, req);

        // ✅ Vérifier conflit de terrain APRÈS avoir attaché la location
        if (!event.isOnline() && location != null && req.getStartDate() != null && req.getEndDate() != null) {
            checkLocationConflict(location.getId(), req.getStartDate(), req.getEndDate(), null);
        }

        attachCampaign(event, req, clubId);

        return eventRepository.save(event);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    public Event updateEvent(Long id, EventRequest req, Long clubId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Event not found: " + id));

        if (event.getClub() == null || !event.getClub().getId().equals(clubId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Not allowed: event belongs to another club");
        }

        mapRequest(event, req, true);
        validateEventMode(event, req);
        Location location = attachLocation(event, req);

        // ✅ Vérifier conflit de terrain en excluant cet event lui-même
        LocalDateTime newStart = req.getStartDate() != null ? req.getStartDate() : event.getStartDate();
        LocalDateTime newEnd   = req.getEndDate()   != null ? req.getEndDate()   : event.getEndDate();
        Long locationId = location != null ? location.getId()
                : (event.getLocation() != null ? event.getLocation().getId() : null);

        if (!event.isOnline() && locationId != null && newStart != null && newEnd != null) {
            checkLocationConflict(locationId, newStart, newEnd, id);
        }

        if (req.getCampaignId() != null) {
            attachCampaign(event, req, clubId);
        } else {
            event.setCampaign(null);
        }

        return eventRepository.save(event);
    }



    // ═══════════════════════════════════════════════════════════════════════
    // FIND
    // ═══════════════════════════════════════════════════════════════════════

    public List<Event> findByClubId(Long clubId) {
        return eventRepository.findByClubId(clubId);
    }

    public List<Event> getAllPublicEvents() {
        return eventRepository.findAll().stream()
                .filter(e -> e.getStatus() != EventStatus.CANCELLED)
                .toList();
    }

    public List<Event> getAllAccessibleEvents(Long clubId) {
        return eventRepository.findAll().stream()
                .filter(e -> e.getStatus() != EventStatus.CANCELLED)
                .filter(e -> {
                    if (e.getCampaign() == null) return true;
                    Campaign c = e.getCampaign();
                    if (c.getVisibility() == CampaignVisibility.PUBLIC) return true;
                    if (c.getVisibility() == CampaignVisibility.PRIVATE) {
                        return c.getOwnerClub() != null && c.getOwnerClub().getId().equals(clubId);
                    }
                    return campaignAccessRepository.findByCampaign_IdAndClub_Id(c.getId(), clubId)
                            .map(ca -> ca.getPermissions() != null
                                    && ca.getPermissions().contains(CampaignPermission.VIEW))
                            .orElse(false);
                })
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PARTICIPANTS COUNT
    // ═══════════════════════════════════════════════════════════════════════

    public void updateParticipantsCount(Long eventId) {
        long count = eventParticipantRepository.countByEventIdAndStatusNot(
                eventId, ParticipationStatus.CANCELLED);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Event not found: " + eventId));

        event.setParticipantsCount((int) count);
        eventRepository.save(event);

        // ✅ Sync totalParticipants de la campagne liée — logique dans le service
        if (event.getCampaign() != null) {
            Long campaignId   = event.getCampaign().getId();
            Long totalLong    = eventParticipantRepository.countParticipantsByCampaignId(campaignId);
            int  total        = totalLong != null ? totalLong.intValue() : 0;

            campaignRepository.findById(campaignId).ifPresent(campaign -> {
                campaign.setTotalParticipants(total);
                campaign.setCurrentParticipants(total);
                campaignRepository.save(campaign);
            });
        }
    }
    // ═══════════════════════════════════════════════════════════════════════
    // WAITING LIST — scheduler (sweep toutes les 30s)
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(fixedRate = 10_000)
    public void processWaitingLists() {
        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            try {
                promoteFromWaitingList(event.getId());
            } catch (Exception e) {
                log.warn("[WaitingList] Error processing event {}: {}", event.getId(), e.getMessage());
            }
        }
    }

    public synchronized void promoteFromWaitingList(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Event not found: " + eventId));

        long active = eventParticipantRepository.countByEventIdAndStatusNot(
                eventId, ParticipationStatus.CANCELLED);

        if (event.getCapacity() != null && event.getCapacity() > 0 && active >= event.getCapacity()) {
            return; // toujours plein
        }

        List<EventWaitingList> queue =
                waitingListRepository.findByEventIdOrderByJoinedAtAsc(eventId);

        if (queue.isEmpty()) return;

        EventWaitingList first  = queue.get(0);
        Long             userId = first.getUser().getId();

        // Guard : déjà promu par un appel concurrent
        boolean alreadyPromoted = eventParticipantRepository
                .findByEventIdAndUserId(eventId, userId)
                .map(p -> p.getStatus() != ParticipationStatus.CANCELLED)
                .orElse(false);

        if (alreadyPromoted) {
            waitingListRepository.delete(first);
            return;
        }

        // ✅ Vérifier conflit de planning pour le participant promu
        List<Event> conflicts = eventRepository.findOverlappingEventsForUser(
                userId, event.getStartDate(), event.getEndDate(), eventId);

        if (!conflicts.isEmpty()) {
            log.warn("[WaitingList] User {} a un conflit de planning — promotion ignorée pour event '{}'",
                    userId, event.getTitle());
            waitingListRepository.delete(first);
            // Optionnel : notifier l'utilisateur par SMS
            String phone = normalizePhone(first.getUser().getPhone());
            if (phone != null) {
                smsService.sendSms(phone,
                        "⚠️ Vous avez été retiré de la liste d'attente de '"
                                + event.getTitle()
                                + "' : conflit avec un autre événement.");
            }
            return;
        }

        EventParticipant p = EventParticipant.builder()
                .event(event)
                .user(first.getUser())
                .status(ParticipationStatus.REGISTERED)
                .reservedSeats(1)
                .wantsReminder(true)
                .reminderSent(false)
                .build();

        eventParticipantRepository.save(p);
        waitingListRepository.delete(first);
        updateParticipantsCount(eventId);

        String normalizedPhone = normalizePhone(first.getUser().getPhone());
        if (normalizedPhone != null) {
            boolean sent = smsService.sendSms(normalizedPhone,
                    "🎉 Place libérée ! Vous êtes inscrit à : " + event.getTitle());
            if (!sent) {
                log.error("[WaitingList] ❌ SMS de promotion non envoyé à l'utilisateur {}", userId);
            }
        } else {
            log.warn("[WaitingList] Numéro invalide ou absent pour l'utilisateur {} — SMS ignoré", userId);
        }

        log.info("[WaitingList] User {} promu et inscrit pour l'événement '{}'", userId, event.getTitle());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AUTO STATUS — toutes les minutes
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(fixedRate = 60_000)
    public void updateEventStatusAutomatically() {
        List<Event> events = eventRepository.findAll();
        LocalDateTime now  = LocalDateTime.now();

        for (Event e : events) {
            if (e.getStatus() == EventStatus.CANCELLED) continue;

            if (e.getEndDate() != null && e.getEndDate().isBefore(now)) {
                e.setStatus(EventStatus.COMPLETED);
            } else if (e.getStartDate() != null && e.getStartDate().isBefore(now)) {
                e.setStatus(EventStatus.ONGOING);
            } else {
                if (e.getStatus() == EventStatus.ONGOING) {
                    e.setStatus(EventStatus.PLANNED);
                }
            }
        }

        eventRepository.saveAll(events);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * ✅ Vérifie qu'aucun autre événement non annulé n'utilise le même terrain
     *    sur la plage [startDate, endDate]. Lance une ResponseStatusException 409
     *    si un conflit est détecté.
     *
     * @param excludeId  null à la création, id de l'événement en cours de modification
     */
    private void checkLocationConflict(Long locationId,
                                       LocalDateTime startDate,
                                       LocalDateTime endDate,
                                       Long excludeId) {
        List<Event> conflicts = eventRepository.findConflictingEventsOnLocation(
                locationId, startDate, endDate, excludeId);

        if (!conflicts.isEmpty()) {
            Event conflict = conflicts.get(0);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le terrain est déjà réservé du "
                            + conflict.getStartDate() + " au " + conflict.getEndDate()
                            + " pour l'événement : " + conflict.getTitle());
        }
    }

    private void mapRequest(Event event, EventRequest req, boolean isEdit) {
        if (req.getTitle()       != null) event.setTitle(req.getTitle());
        if (req.getDescription() != null) event.setDescription(req.getDescription());
        if (req.getStartDate()   != null) event.setStartDate(req.getStartDate());
        if (req.getEndDate()     != null) event.setEndDate(req.getEndDate());
        if (req.getCapacity()    != null) event.setCapacity(req.getCapacity());
        if (req.getImageUrl()    != null) event.setImageUrl(req.getImageUrl());
        if (req.getCategory()    != null) event.setCategory(req.getCategory());
        if (req.getEventType()   != null) event.setEventType(req.getEventType());

        if (isEdit && req.getStatus() != null) {
            event.setStatus(req.getStatus());
        }

        if (event.getEventType() == EventType.ONLINE) {
            event.setMeetingUrl(resolveMeetingUrl(req, event));
        } else {
            event.setMeetingUrl(null);
        }
    }

    /**
     * Attache ou crée la Location. Retourne la Location résultante (ou null).
     */
    private Location attachLocation(Event event, EventRequest req) {
        if (event.getEventType() == EventType.ONLINE) {
            event.setLocation(null);
            return null;
        }

        if (req.getLocation() == null || req.getLocation().isBlank()) return null;

        Location location = locationRepository.findByName(req.getLocation());

        if (location == null) {
            location = new Location();
            location.setName(req.getLocation());
            location.setAddress(req.getLocation());
            location.setLatitude(req.getLatitude());
            location.setLongitude(req.getLongitude());
        } else {
            if (req.getLatitude()  != null) location.setLatitude(req.getLatitude());
            if (req.getLongitude() != null) location.setLongitude(req.getLongitude());
        }

        location = locationRepository.save(location);
        event.setLocation(location);
        return location;
    }

    private void validateEventMode(Event event, EventRequest req) {
        EventType eventType = event.getEventType() != null ? event.getEventType() : EventType.OFFLINE;

        if (eventType == EventType.ONLINE) {
            event.setLocation(null);
            event.setMeetingUrl(resolveMeetingUrl(req, event));
            return;
        }

        if (req.getLocation() == null || req.getLocation().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Location is required for offline events");
        }
        event.setMeetingUrl(null);
    }

    private String resolveMeetingUrl(EventRequest req, Event event) {
        if (req.getMeetingUrl() != null && !req.getMeetingUrl().isBlank()) {
            return req.getMeetingUrl().trim();
        }

        String slugBase = (event.getTitle() == null || event.getTitle().isBlank())
                ? "cluverse-event"
                : event.getTitle();

        return "https://meet.jit.si/" + slugify(slugBase) + "-" + System.currentTimeMillis();
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "cluverse-event" : normalized;
    }

    private void attachCampaign(Event event, EventRequest req, Long clubId) {
        if (req.getCampaignId() == null) return;

        Campaign campaign = campaignRepository.findById(req.getCampaignId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Campaign not found: " + req.getCampaignId()));

        CampaignStatus status = campaign.getStatus();
        if (status != CampaignStatus.PLANNED && status != CampaignStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'ajout d'événements est interdit car la campagne est dans un état \""
                    + status.name() + "\". Seuls les statuts PLANNED et ACTIVE sont autorisés.");
        }

        if (campaign.getVisibility() == CampaignVisibility.PRIVATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cannot attach event to a private campaign");
        }

        if (!campaignService.canAddEventToCampaign(campaign, clubId, false)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Not allowed to attach event to this campaign");
        }

        if (!isEventCompatibleWithCampaign(event, campaign)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Event dates must be within campaign dates");
        }

        event.setCampaign(campaign);
    }

    private boolean isEventCompatibleWithCampaign(Event event, Campaign campaign) {
        if (event.getStartDate() == null || event.getEndDate() == null)       return true;
        if (campaign.getStartDate() == null || campaign.getEndDate() == null) return true;

        LocalDate eventStart    = event.getStartDate().toLocalDate();
        LocalDate eventEnd      = event.getEndDate().toLocalDate();
        LocalDate campaignStart = campaign.getStartDate().toLocalDate();
        LocalDate campaignEnd   = campaign.getEndDate().toLocalDate();

        return !eventStart.isBefore(campaignStart) && !eventEnd.isAfter(campaignEnd);
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

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE EVENT
    // ═══════════════════════════════════════════════════════════════════════

    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        eventRepository.delete(event);
        log.info("[EVENT_DELETE] Event {} deleted", eventId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCEL EVENT (Set status to CANCELLED + notify participants via SMS)
    // ═══════════════════════════════════════════════════════════════════════

    public Event cancelEvent(Long eventId, Long clubId) {
        log.info("[EVENT_CANCEL] Attempting to cancel event ID: {} for club: {}", eventId, clubId);
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        // Vérifier les permissions
        if (event.getClub() == null || !event.getClub().getId().equals(clubId)) {
            log.warn("[EVENT_CANCEL] Access denied: event club {} != user club {}", 
                    event.getClub() != null ? event.getClub().getId() : null, clubId);
            throw new RuntimeException("Not allowed to cancel this event");
        }

        // Changer le statut
        event.setStatus(EventStatus.CANCELLED);
        Event savedEvent = eventRepository.save(event);
        log.info("[EVENT_CANCEL] Event '{}' (ID: {}) status updated to CANCELLED", 
                event.getTitle(), eventId);

        // Envoyer les SMS aux participants
        notifyParticipantsOfCancellation(event);

        return savedEvent;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NOTIFY PARTICIPANTS VIA SMS
    // ═══════════════════════════════════════════════════════════════════════

    private void notifyParticipantsOfCancellation(Event event) {
        log.info("[EVENT_CANCEL_SMS] Starting cancellation notifications for event: '{}'", event.getTitle());
        
        // Récupérer tous les participants
        List<EventParticipant> participants = eventParticipantRepository.findByEventId(event.getId());

        if (participants == null || participants.isEmpty()) {
            log.info("[EVENT_CANCEL_SMS] No participants to notify for event {}", event.getTitle());
            return;
        }

        log.info("[EVENT_CANCEL_SMS] Sending cancellation SMS to {} participants", participants.size());

        int successCount = 0;
        int failureCount = 0;
        
        for (EventParticipant participant : participants) {
            try {
                String phone = participant.getUser() != null ? participant.getUser().getPhone() : null;
                if (phone == null || phone.isBlank()) {
                    log.warn("[EVENT_CANCEL_SMS] No phone number for participant {}", participant.getId());
                    failureCount++;
                    continue;
                }

                String normalizedPhone = normalizePhone(phone);
                if (normalizedPhone == null) {
                    log.warn("[EVENT_CANCEL_SMS] Invalid phone number format: {}", phone);
                    failureCount++;
                    continue;
                }

                String message = String.format(
                        "Bonjour,\n\nL'événement \"%s\" prévu le %s a été annulé.\n\nCordialement,\nCluverse",
                        event.getTitle(),
                        event.getStartDate()
                );

                // Envoyer via SMS service
                smsService.sendSms(normalizedPhone, message);
                log.info("[EVENT_CANCEL_SMS] SMS sent successfully to {}", normalizedPhone);
                successCount++;

            } catch (Exception e) {
                log.error("[EVENT_CANCEL_SMS] Failed to send SMS to participant {}: {}", 
                        participant.getId(), e.getMessage(), e);
                failureCount++;
            }
        }
        
        log.info("[EVENT_CANCEL_SMS] Completed: {} successful, {} failed out of {} participants", 
                successCount, failureCount, participants.size());
    }
}
