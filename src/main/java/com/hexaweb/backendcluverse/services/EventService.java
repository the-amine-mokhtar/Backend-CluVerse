package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.EventRequest;
import com.hexaweb.backendcluverse.dto.EventResponseDTO;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.event.Campaign;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.CampaignRepository;
import com.hexaweb.backendcluverse.repositories.EventParticipantRepository;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService extends EntityServiceImpl<Event, Long> {
    private final EventParticipantRepository eventParticipantRepository;
    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final CampaignRepository campaignRepository;

    public EventService(EventRepository repository,
                        ClubRepository clubRepository,
                        CampaignRepository campaignRepository,
                        EventParticipantRepository eventParticipantRepository) {
        super(repository);
        this.eventRepository = repository;
        this.clubRepository = clubRepository;
        this.campaignRepository = campaignRepository;
        this.eventParticipantRepository = eventParticipantRepository;
    }

    private EventStatus parseStatus(String status) {
        if (status == null || "null".equals(status) || status.trim().isEmpty()) {
            return EventStatus.PLANNED;
        }
        try {
            return EventStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            // ✅ If status is invalid, default to PLANNED instead of throwing exception
            return EventStatus.PLANNED;
        }
    }

    private EventStatus computeStatus(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            return EventStatus.CANCELLED;
        }
        if (event.getEndDate() != null && event.getEndDate().isBefore(LocalDateTime.now())) {
            return EventStatus.COMPLETED;
        }
        return EventStatus.AVAILABLE;
    }

    public void updateParticipantsCount(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        int count = eventParticipantRepository.countByEventIdAndStatusNotCancelled(eventId);
        event.setParticipantsCount(count);

        eventRepository.save(event);
    }

    public Event createEvent(EventRequest req, Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));

        Event event = new Event();
        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setLocation(req.getLocation());
        event.setStartDate(req.getStartDate());
        event.setEndDate(req.getEndDate());
        event.setStatus(parseStatus(String.valueOf(req.getStatus())));
        event.setCapacity(req.getCapacity());
        event.setClub(club);
        event.setImageUrl(req.getImageUrl());
        event.setCategory(req.getCategory());
        event.setIsPaid(req.getIsPaid() != null ? req.getIsPaid() : false);
        event.setPrice(req.getPrice());
        event.setLatitude(req.getLatitude());
        event.setLongitude(req.getLongitude());
        
        if (req.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findById(req.getCampaignId())
                    .orElseThrow(() -> new RuntimeException("Campaign not found"));
            event.setCampaign(campaign);
        }


        return eventRepository.save(event);
    }

    public Event updateEvent(Long id, EventRequest req) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (req.getTitle() != null) event.setTitle(req.getTitle());
        if (req.getDescription() != null) event.setDescription(req.getDescription());
        if (req.getLocation() != null) event.setLocation(req.getLocation());
        if (req.getStartDate() != null) event.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) event.setEndDate(req.getEndDate());
        if (req.getStatus() != null) event.setStatus(parseStatus(String.valueOf(req.getStatus())));
        if (req.getCapacity() != null) event.setCapacity(req.getCapacity());
        if (req.getImageUrl() != null) event.setImageUrl(req.getImageUrl());
        if (req.getCategory() != null) event.setCategory(req.getCategory());
        if (req.getIsPaid() != null) event.setIsPaid(req.getIsPaid());
        if (req.getPrice() != null) event.setPrice(req.getPrice());
        if (req.getLatitude() != null) event.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) event.setLongitude(req.getLongitude());

        if (req.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findById(req.getCampaignId())
                    .orElseThrow(() -> new RuntimeException("Campaign not found"));
            event.setCampaign(campaign);
        } else {
            event.setCampaign(null);
        }

        return eventRepository.save(event);
    }

    public List<Event> findByClubId(Long clubId) {
        List<Event> events = eventRepository.findByClubId(clubId);

        // ✅ Normalize any invalid status values in existing data
        for (Event event : events) {
            if (event.getStatus() != null) {
                try {
                    // Try to validate the current status
                    EventStatus.valueOf(event.getStatus().name());
                } catch (Exception e) {
                    // If invalid, set to PLANNED and save
                    event.setStatus(EventStatus.PLANNED);
                    eventRepository.save(event);
                }
            }
        }

        return events;
    }

    /**
     * Obtenir les événements du club avec les DTOs enrichis
     */
    public List<EventResponseDTO> findByClubIdWithDetails(Long clubId) {
        return findByClubId(clubId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertir Event à EventResponseDTO avec calcul du statut de capacité
     */
    public EventResponseDTO convertToResponseDTO(Event event) {
        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .imageUrl(event.getImageUrl())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .capacity(event.getCapacity())
                .participantsCount(event.getParticipantsCount())
                .status(computeStatus(event))
                .category(event.getCategory())
                .clubId(event.getClub() != null ? event.getClub().getId() : null)
                .campaignId(event.getCampaign() != null ? event.getCampaign().getId() : null)
                .isPaid(event.getIsPaid())
                .price(event.getPrice())
                .capacityStatus(event.getCapacityStatus())
                .availableSeats(event.getAvailableSeats())

                .build();
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    public List<Object[]> getStatsByMonth(Long clubId) {
        return eventRepository.countEventsByMonth(clubId);
    }

    public List<Event> findCancelledEventsByClubId(Long clubId) {
        return eventRepository.findByClubId(clubId).stream()
                .filter(e -> e.getStatus() == EventStatus.CANCELLED)
                .toList();
    }

    public void cancelEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setStatus(EventStatus.CANCELLED);
        event.setDeletedAt(LocalDateTime.now());
        eventRepository.save(event);
    }
}