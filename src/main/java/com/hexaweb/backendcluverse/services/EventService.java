package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.EventParticipantRequest;
import com.hexaweb.backendcluverse.dto.EventRequest;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.*;
import com.hexaweb.backendcluverse.enumerations.*;
import com.hexaweb.backendcluverse.repositories.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class EventService extends EntityServiceImpl<Event, Long> {

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

    // ── NOTE: Reminder scheduling has been moved entirely to EventReminderService.
    //          This class no longer contains @Scheduled methods to avoid double-sending SMS.

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
                .orElseThrow(() -> new RuntimeException("Club not found: " + clubId));

        Event event = new Event();
        mapRequest(event, req, false);
        event.setStatus(EventStatus.PLANNED);
        event.setClub(club);

        attachLocation(event, req);
        attachCampaign(event, req, clubId);

        return eventRepository.save(event);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    public Event updateEvent(Long id, EventRequest req, Long clubId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        if (event.getClub() == null || !event.getClub().getId().equals(clubId)) {
            throw new RuntimeException("Not allowed: event belongs to another club");
        }

        mapRequest(event, req, true);
        attachLocation(event, req);

        if (req.getCampaignId() != null) {
            attachCampaign(event, req, clubId);
        } else {
            event.setCampaign(null);
        }

        return eventRepository.save(event);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════

    public void deleteEvent(Long id) {
        eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        eventParticipantRepository.deleteByEventId(id);
        waitingListRepository.deleteByEventId(id);
        eventRepository.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCEL
    // ═══════════════════════════════════════════════════════════════════════

    public void cancelEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        event.setStatus(EventStatus.CANCELLED);
        event.setDeletedAt(LocalDateTime.now());
        eventRepository.save(event);
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

    /**
     * Returns all events accessible to a club, respecting campaign visibility rules:
     *   - No campaign           → always visible
     *   - PUBLIC campaign       → always visible
     *   - PRIVATE campaign      → only visible to owner club
     *   - SHARED campaign       → visible if club has VIEW permission
     */
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

                    // SHARED
                    return campaignAccessRepository.findByCampaignIdAndClubId(c.getId(), clubId)
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
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        event.setParticipantsCount((int) count);
        eventRepository.save(event);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WAITING LIST
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(fixedRate = 30000) // toutes les 30 secondes
    public void processWaitingLists() {

        List<Event> events = eventRepository.findAll();

        for (Event event : events) {
            try {
                promoteFromWaitingList(event.getId());
            } catch (Exception e) {
                // sécurité pour éviter crash scheduler
            }
        }
    }
    public synchronized void promoteFromWaitingList(Long eventId) {
        promoteFromWaitingListSafe(eventId);
    }
    // 🔥 PROMOTION SAFE + CENTRALIZED
    private void promoteFromWaitingListSafe(Long eventId)  {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        long active = eventParticipantRepository.countByEventIdAndStatusNot(
                eventId, ParticipationStatus.CANCELLED);

        if (event.getCapacity() != null && active >= event.getCapacity()) return;

        List<EventWaitingList> queue =
                waitingListRepository.findByEventIdOrderByJoinedAtAsc(eventId);

        if (queue.isEmpty()) return;

        EventWaitingList first = queue.get(0);

        boolean alreadyPromoted = eventParticipantRepository
                .findByEventIdAndUserId(eventId, first.getUser().getId())
                .isPresent();

        if (alreadyPromoted) {
            waitingListRepository.delete(first);
            return;
        }

        EventParticipant p = EventParticipant.builder()
                .event(event)
                .user(first.getUser())
                .status(ParticipationStatus.REGISTERED)
                .reservedSeats(1)
                .build();

        eventParticipantRepository.save(p);
        waitingListRepository.delete(first);

        updateParticipantsCount(eventId);

        smsService.sendSms(first.getUser().getPhone(),
                "🎉 Place libérée ! Vous êtes inscrit à : " + event.getTitle());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AUTO STATUS — runs every minute
    // ═══════════════════════════════════════════════════════════════════════

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60_000)
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

    private void mapRequest(Event event, EventRequest req, boolean isEdit) {
        if (req.getTitle()       != null) event.setTitle(req.getTitle());
        if (req.getDescription() != null) event.setDescription(req.getDescription());
        if (req.getStartDate()   != null) event.setStartDate(req.getStartDate());
        if (req.getEndDate()     != null) event.setEndDate(req.getEndDate());
        if (req.getCapacity()    != null) event.setCapacity(req.getCapacity());
        if (req.getImageUrl()    != null) event.setImageUrl(req.getImageUrl());
        if (req.getCategory()    != null) event.setCategory(req.getCategory());

        if (req.getIsPaid() != null) {
            event.setIsPaid(req.getIsPaid());
            if (req.getIsPaid()) {
                if (req.getPrice() != null && req.getPrice() > 0) {
                    event.setPrice(req.getPrice());
                }
            } else {
                event.setPrice(null);
            }
        }

        if (req.getPrice() != null && req.getIsPaid() != null && req.getIsPaid()) {
            event.setPrice(req.getPrice());
        }

        if (isEdit && req.getStatus() != null) {
            event.setStatus(req.getStatus());
        }
    }

    private void attachLocation(Event event, EventRequest req) {
        if (req.getLocation() == null || req.getLocation().isBlank()) return;

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
    }

    private void attachCampaign(Event event, EventRequest req, Long clubId) {
        if (req.getCampaignId() == null) return;

        Campaign campaign = campaignRepository.findById(req.getCampaignId())
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + req.getCampaignId()));

        if (campaign.getVisibility() == CampaignVisibility.PRIVATE) {
            throw new RuntimeException("Cannot attach event to a private campaign");
        }

        if (!campaignService.canAddEventToCampaign(campaign, clubId, false)) {
            throw new RuntimeException("Not allowed to attach event to this campaign");
        }

        if (!isEventCompatibleWithCampaign(event, campaign)) {
            throw new RuntimeException("Event dates must be within campaign dates");
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
}