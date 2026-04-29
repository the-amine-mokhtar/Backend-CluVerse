package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.CampaignRequest;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.Campaign;
import com.hexaweb.backendcluverse.entities.event.CampaignAccess;
import com.hexaweb.backendcluverse.entities.event.CampaignView;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.enumerations.CampaignPermission;
import com.hexaweb.backendcluverse.enumerations.CampaignStatus;
import com.hexaweb.backendcluverse.enumerations.CampaignVisibility;
import com.hexaweb.backendcluverse.enumerations.RoleType;
import com.hexaweb.backendcluverse.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CampaignService extends EntityServiceImpl<Campaign, Long> {

    private final CampaignRepository      campaignRepository;
    private final EventRepository         eventRepository;
    private final EventParticipantRepository participantRepository;
    private final CloudinaryService       cloudinaryService;
    private final CampaignAccessRepository campaignAccessRepository;
    private final ClubRepository          clubRepository;
    private final CampaignViewRepository  campaignViewRepository;
    private final UserRepository          userRepository;
    private final JavaMailSender          mailSender;
    @Value("${spring.mail.username}")
    private String fromAddress;

    public CampaignService(CampaignRepository repository,
                           EventRepository eventRepository,
                           EventParticipantRepository participantRepository,
                           CloudinaryService cloudinaryService,
                           CampaignAccessRepository campaignAccessRepository,
                           ClubRepository clubRepository,
                           CampaignViewRepository campaignViewRepository,
                           UserRepository userRepository,
                           JavaMailSender mailSender) {
        super(repository);
        this.campaignRepository      = repository;
        this.eventRepository         = eventRepository;
        this.participantRepository   = participantRepository;
        this.cloudinaryService       = cloudinaryService;
        this.campaignAccessRepository = campaignAccessRepository;
        this.clubRepository          = clubRepository;
        this.campaignViewRepository  = campaignViewRepository;
        this.userRepository          = userRepository;
        this.mailSender              = mailSender;
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET ALL
    // ═══════════════════════════════════════════════════════════════════

    public List<Campaign> getAllCampaigns(Long clubId, boolean isSuperAdmin) {
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository.findAll().stream()
                .filter(c -> canViewCampaign(clubId, c, isSuperAdmin))
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(now))
                .map(c -> {
                    refreshDerivedFields(c);
                    c.setCanAddEvent(canAddEventToCampaign(c, clubId, isSuperAdmin));
                    return c;
                })
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET BY ID
    // ═══════════════════════════════════════════════════════════════════

    public Campaign getCampaignById(Long id, Long clubId, boolean isSuperAdmin) {
        Campaign c = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        if (!canViewCampaign(clubId, c, isSuperAdmin)) {
            throw new RuntimeException("Access denied");
        }
        refreshDerivedFields(c);
        c.setCanAddEvent(canAddEventToCampaign(c, clubId, isSuperAdmin));
        return c;
    }

    // ═══════════════════════════════════════════════════════════════════
    // RECORD VIEW
    // ═══════════════════════════════════════════════════════════════════

    public Campaign recordCampaignView(Long campaignId, Long userId) {
        Campaign c = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (userId == null) {
            refreshDerivedFields(c);
            return c;
        }

        boolean alreadyViewed = campaignViewRepository
                .existsByCampaignIdAndUserId(campaignId, userId);

        if (!alreadyViewed) {
            CampaignView view = CampaignView.builder()
                    .campaign(c)
                    .user(userRepository.getReferenceById(userId))
                    .build();
            campaignViewRepository.save(view);
            c.setViews((c.getViews() == null ? 0 : c.getViews()) + 1);
            campaignRepository.save(c);
        }

        refreshDerivedFields(c);
        return c;
    }

    /**
     * Enregistre une vue de campagne si l'utilisateur est admin ou président
     * @param campaignId ID de la campagne
     * @param userId ID de l'utilisateur (peut être null)
     * @param clubId ID du club de l'utilisateur
     * @param isAdminOrPresident true si l'utilisateur est admin ou président
     */
    public Campaign recordCampaignView(Long campaignId, Long userId, Long clubId, boolean isAdminOrPresident) {
        Campaign c = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        // Only record view if user is admin or president
        if (!isAdminOrPresident || userId == null) {
            refreshDerivedFields(c);
            return c;
        }

        boolean alreadyViewed = campaignViewRepository
                .existsByCampaignIdAndUserId(campaignId, userId);

        if (!alreadyViewed) {
            CampaignView view = CampaignView.builder()
                    .campaign(c)
                    .user(userRepository.getReferenceById(userId))
                    .build();
            campaignViewRepository.save(view);
            c.setViews((c.getViews() == null ? 0 : c.getViews()) + 1);
            campaignRepository.save(c);
            log.info("[CAMPAIGN_VIEW] Campaign '{}' viewed by user {} (admin/president)", campaignId, userId);
        }

        refreshDerivedFields(c);
        return c;
    }

    // ═══════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════

    public Campaign createCampaign(CampaignRequest req, Long clubId, boolean isSuperAdmin) {
        Club ownerClub = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found: " + clubId));

        Campaign campaign = new Campaign();

        if (req.getImageFile() != null && !req.getImageFile().isEmpty()) {
            try {
                campaign.setImageUrl(cloudinaryService.uploadCampaignImage(req.getImageFile()));
            } catch (Exception e) {
                throw new RuntimeException("Image upload failed: " + e.getMessage());
            }
        }

        campaign.setOwnerClub(ownerClub);
        campaign.setVisibility(
                req.getVisibility() != null ? req.getVisibility() : CampaignVisibility.SHARED);

        applyRequestToCampaign(campaign, req);
        validateDates(campaign);
        refreshDerivedFields(campaign);

        return campaignRepository.save(campaign);
    }

    // ═══════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════

    public Campaign updateCampaign(Long id, CampaignRequest req,
                                   Long clubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));

        if (!isSuperAdmin && (campaign.getOwnerClub() == null
                || !campaign.getOwnerClub().getId().equals(clubId))) {
            throw new RuntimeException("You can only update your own campaigns");
        }

        if (req.getImageFile() != null && !req.getImageFile().isEmpty()) {
            try {
                campaign.setImageUrl(cloudinaryService.uploadCampaignImage(req.getImageFile()));
            } catch (Exception e) {
                throw new RuntimeException("Image upload failed: " + e.getMessage());
            }
        }

        if (req.getVisibility() != null) {
            campaign.setVisibility(req.getVisibility());
        }

        applyRequestToCampaign(campaign, req);
        validateDates(campaign);
        refreshDerivedFields(campaign);

        return campaignRepository.save(campaign);
    }

    // ═══════════════════════════════════════════════════════════════════
    // UPDATE STATUS
    // ═══════════════════════════════════════════════════════════════════

    public Campaign updateCampaignStatus(Long id, CampaignStatus newStatus,
                                          Long clubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));

        if (!isSuperAdmin && (campaign.getOwnerClub() == null
                || !campaign.getOwnerClub().getId().equals(clubId))) {
            throw new RuntimeException("You can only update your own campaigns");
        }

        // 🔐 Ne pas permettre de changer le statut si verrouillé
        if (campaign.getStatus() == CampaignStatus.LOCKED) {
            throw new RuntimeException("Cannot change status of a LOCKED campaign");
        }

        campaign.setStatus(newStatus);
        campaign = campaignRepository.save(campaign);
        
        refreshDerivedFields(campaign);
        
        log.info("[STATUS_UPDATE] Campaign '{}' (ID: {}) status changed to: {}", 
                campaign.getTitle(), id, newStatus);
        
        return campaign;
    }

    // ═══════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public Map<String, Object> deleteCampaign(Long id, Long clubId, boolean isSuperAdmin) {
        Map<String, Object> result = new java.util.HashMap<>();
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        boolean isOwner = campaign.getOwnerClub() != null
                && campaign.getOwnerClub().getId().equals(clubId);

        boolean hasSharedDeleteAccess = false;
        if (!isOwner && campaign.getVisibility() == CampaignVisibility.SHARED) {
            hasSharedDeleteAccess = campaignAccessRepository
                    .findByCampaign_IdAndClub_Id(id, clubId)
                    .map(a -> a.getPermissions().contains(CampaignPermission.DELETE)
                            || a.getPermissions().contains(CampaignPermission.MANAGE))
                    .orElse(false);
        }

        if (!isSuperAdmin && !isOwner && !hasSharedDeleteAccess) {
            throw new RuntimeException("Not allowed");
        }

        // ✅ Vérification dans le SERVICE — pas dans l'entité
        if (campaignHasParticipants(id)) {
            int eventsCount       = (int) eventRepository.countByCampaignId(id);
            Long participantsLong = participantRepository.countParticipantsByCampaignId(id);
            int totalParticipants = participantsLong != null ? participantsLong.intValue() : 0;

            campaign.setEventsCount(eventsCount);
            campaign.setTotalParticipants(totalParticipants);
            campaign.setStatus(CampaignStatus.LOCKED);
            campaignRepository.save(campaign);

            notifyEventManagersOfLock(campaign, eventsCount, totalParticipants);

            result.put("action", "LOCKED");
            result.put("message", "CAMPAIGN_LOCKED: Cette campagne contient "
                    + eventsCount + " événement(s) avec "
                    + totalParticipants + " participant(s). Elle a été verrouillée.");
            result.put("eventsCount", eventsCount);
            result.put("participantsCount", totalParticipants);
            return result;
        }

        // Suppression normale
        List<Event> events = eventRepository.findByCampaignId(id);
        for (Event e : events) {
            e.setCampaign(null);
            eventRepository.save(e);
        }

        List<CampaignAccess> accesses = campaignAccessRepository.findByCampaign_Id(id);
        campaignAccessRepository.deleteAll(accesses);
        campaignViewRepository.deleteAllByCampaignId(id);
        campaignRepository.delete(campaign);

        result.put("action", "DELETED");
        result.put("message", "Campaign deleted successfully");
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // CANCEL CAMPAIGN (Set status to CANCELLED + Notify Event Managers)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public Campaign cancelCampaign(Long id, Long clubId, boolean isSuperAdmin) {
        log.info("[CAMPAIGN_CANCEL] Attempting to cancel campaign ID: {} by club: {} (isSuperAdmin: {})", 
                id, clubId, isSuperAdmin);
        
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        boolean isOwner = campaign.getOwnerClub() != null
                && campaign.getOwnerClub().getId().equals(clubId);

        log.info("[CAMPAIGN_CANCEL] Campaign '{}' - isOwner: {}, visibility: {}", 
                campaign.getTitle(), isOwner, campaign.getVisibility());

        if (!isSuperAdmin && !isOwner) {
            log.warn("[CAMPAIGN_CANCEL] Access denied for club {} to cancel campaign {}", clubId, id);
            throw new RuntimeException("Not allowed to cancel this campaign");
        }
if (campaign.getStatus() == CampaignStatus.CANCELLED) {
    throw new RuntimeException("Campaign is already cancelled");
}
if (campaign.getStatus() == CampaignStatus.LOCKED) {
    throw new RuntimeException("Cannot cancel a LOCKED campaign");
}
        // Set status to CANCELLED
        campaign.setStatus(CampaignStatus.CANCELLED);
        Campaign savedCampaign = campaignRepository.save(campaign);
        log.info("[CAMPAIGN_CANCEL] Campaign '{}' status updated to CANCELLED", campaign.getTitle());

        // Get all linked events
        List<Event> linkedEvents = eventRepository.findByCampaignId(id);
        log.info("[CAMPAIGN_CANCEL] Found {} linked events", linkedEvents != null ? linkedEvents.size() : 0);

        // Notify event managers of cancellation
        notifyEventManagersOfCancellation(campaign, linkedEvents);
        log.info("[CAMPAIGN_CANCEL] Notifications sent for campaign '{}'", campaign.getTitle());

        return savedCampaign;
    }

    // ═══════════════════════════════════════════════════════════════════
    // EVENTS
    // ═══════════════════════════════════════════════════════════════════

    public List<Event> getCampaignEvents(Long campaignId) {
        return eventRepository.findByCampaignId(campaignId);
    }

    /**
     * Récupère les événements liés à une campagne avec vérification de permissions
     * Seuls les admins et présidents peuvent accéder à tous les événements
     * @param campaignId ID de la campagne
     * @param clubId ID du club de l'utilisateur
     * @param isAdminOrPresident true si l'utilisateur est admin ou président
     * @return Liste des événements filtrés selon les permissions
     */
    public List<Event> getCampaignEvents(Long campaignId, Long clubId, boolean isAdminOrPresident) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        // Admin or President can see all events of the campaign
        if (isAdminOrPresident) {
            return eventRepository.findByCampaignId(campaignId);
        }

        // Non-admins can only see events from their club
        if (campaign.getOwnerClub() != null && campaign.getOwnerClub().getId().equals(clubId)) {
            return eventRepository.findByCampaignId(campaignId);
        }

        log.info("[CAMPAIGN_EVENTS] Access denied for user in club {} to campaign {} events", clubId, campaignId);
        return new ArrayList<>();
    }

    public Event assignEventToCampaign(Long campaignId, Long eventId,
                                       Long clubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!canAddEventToCampaign(campaign, clubId, isSuperAdmin)) {
            throw new RuntimeException("No permission to add event to campaign");
        }

        event.setCampaign(campaign);
        Event saved = eventRepository.save(event);
        syncCampaignCounts(campaignId);
        return saved;
    }

    public Event removeEventFromCampaign(Long campaignId, Long eventId,
                                         Long clubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getCampaign() == null
                || !event.getCampaign().getId().equals(campaignId)) {
            throw new RuntimeException("Event not linked to this campaign");
        }

        if (!isSuperAdmin && (event.getClub() == null
                || !event.getClub().getId().equals(clubId))) {
            throw new RuntimeException("Not allowed");
        }

        event.setCampaign(null);
        Event saved = eventRepository.save(event);
        syncCampaignCounts(campaignId);
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════════
    // PARTICIPANTS
    // ═══════════════════════════════════════════════════════════════════

    public List<?> getParticipants(Long campaignId, Long clubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        if (!canViewCampaign(clubId, campaign, isSuperAdmin)) {
            throw new RuntimeException("Access denied");
        }
        return participantRepository.findByCampaignId(campaignId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // PERMISSIONS
    // ═══════════════════════════════════════════════════════════════════

    public CampaignAccess grantPermission(Long campaignId, Long clubId,
                                          CampaignPermission permission,
                                          Long ownerClubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        if (!isSuperAdmin && (campaign.getOwnerClub() == null
                || !campaign.getOwnerClub().getId().equals(ownerClubId))) {
            throw new RuntimeException("Only campaign owner can manage permissions");
        }
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));
        CampaignAccess access = campaignAccessRepository
                .findByCampaign_IdAndClub_Id(campaignId, clubId)
                .orElse(new CampaignAccess(null, campaign, club, new java.util.HashSet<>()));
        access.getPermissions().add(permission);
        return campaignAccessRepository.save(access);
    }

    public void revokePermission(Long campaignId, Long clubId,
                                 CampaignPermission permission,
                                 Long ownerClubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        if (!isSuperAdmin && (campaign.getOwnerClub() == null
                || !campaign.getOwnerClub().getId().equals(ownerClubId))) {
            throw new RuntimeException("Only campaign owner can manage permissions");
        }
        CampaignAccess access = campaignAccessRepository
                .findByCampaign_IdAndClub_Id(campaignId, clubId)
                .orElseThrow(() -> new RuntimeException("Access not found"));
        access.getPermissions().remove(permission);
        if (access.getPermissions().isEmpty()) {
            campaignAccessRepository.delete(access);
        } else {
            campaignAccessRepository.save(access);
        }
    }

    public List<CampaignAccess> getCampaignAccesses(Long campaignId,
                                                    Long ownerClubId,
                                                    boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        if (!isSuperAdmin && (campaign.getOwnerClub() == null
                || !campaign.getOwnerClub().getId().equals(ownerClubId))) {
            throw new RuntimeException("Only campaign owner can view permissions");
        }
        return campaignAccessRepository.findByCampaign_Id(campaignId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // VISIBILITY RULES
    // ═══════════════════════════════════════════════════════════════════

    public boolean canViewCampaign(Long clubId, Campaign c, boolean isSuperAdmin) {
        if (isSuperAdmin) return true;
        if (c == null)    return false;
        if (c.getOwnerClub() != null && c.getOwnerClub().getId().equals(clubId)) return true;
        if (c.getVisibility() == CampaignVisibility.PUBLIC)  return true;
        if (c.getVisibility() == CampaignVisibility.PRIVATE) return false;
        return campaignAccessRepository
                .findByCampaign_IdAndClub_Id(c.getId(), clubId)
                .map(this::hasViewPermission)
                .orElse(false);
    }

    public boolean canAddEventToCampaign(Campaign campaign, Long clubId, boolean isSuperAdmin) {
        if (isSuperAdmin)    return true;
        if (campaign == null) return false;
        if (campaign.getOwnerClub() != null
                && campaign.getOwnerClub().getId().equals(clubId)) return true;
        if (campaign.getVisibility() == CampaignVisibility.PUBLIC)  return true;
        if (campaign.getVisibility() == CampaignVisibility.PRIVATE) return false;
        return campaignAccessRepository
                .findByCampaign_IdAndClub_Id(campaign.getId(), clubId)
                .map(this::hasAddEventPermission)
                .orElse(false);
    }

    // ═══════════════════════════════════════════════════════════════════
    // TOP 5 / FILTERS
    // ═══════════════════════════════════════════════════════════════════

    public List<Campaign> getTop5CampaignsForEventManager(Long clubId, boolean isSuperAdmin) {
        LocalDateTime now     = LocalDateTime.now();
        List<CampaignAccess> accesses = campaignAccessRepository.findByClub_Id(clubId);

        return campaignRepository.findAll().stream()
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(now))
                .filter(c -> c.getStatus() != CampaignStatus.CANCELLED
                        && c.getStatus() != CampaignStatus.FINISHED)
                .filter(c -> canSeeForTop(c, clubId, accesses))
                .map(c -> {
                    refreshDerivedFields(c);
                    c.setCanAddEvent(canAddEventToCampaign(c, clubId, isSuperAdmin));
                    return c;
                })
                .sorted(this::compareCampaignRanking)
                .limit(5)
                .toList();
    }

    public List<Campaign> getCampaignsForEventForm(Long clubId, boolean isSuperAdmin) {
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository.findAll().stream()
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(now))
                .filter(c -> c.getStatus() != CampaignStatus.CANCELLED
                        && c.getStatus() != CampaignStatus.FINISHED)
                .filter(c -> {
                    if (c.getVisibility() == CampaignVisibility.PUBLIC)  return true;
                    if (c.getVisibility() == CampaignVisibility.SHARED)
                        return campaignAccessRepository
                                .findByCampaign_IdAndClub_Id(c.getId(), clubId).isPresent();
                    if (c.getVisibility() == CampaignVisibility.PRIVATE)
                        return c.getOwnerClub() != null
                                && c.getOwnerClub().getId().equals(clubId);
                    return false;
                })
                .map(c -> {
                    refreshDerivedFields(c);
                    c.setCanAddEvent(canAddEventToCampaign(c, clubId, isSuperAdmin));
                    return c;
                })
                .sorted((c1, c2) -> {
                    LocalDateTime d1 = c1.getStartDate() != null ? c1.getStartDate() : LocalDateTime.MAX;
                    LocalDateTime d2 = c2.getStartDate() != null ? c2.getStartDate() : LocalDateTime.MAX;
                    return d1.compareTo(d2);
                })
                .collect(Collectors.toList());
    }

    public List<Campaign> getCampaignsSharedWithClub(Long clubId) {
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository.findAll().stream()
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(now))
                .filter(c -> c.getStatus() != CampaignStatus.CANCELLED
                        && c.getStatus() != CampaignStatus.FINISHED)
                .filter(c -> {
                    if (c.getVisibility() == CampaignVisibility.PUBLIC) return true;
                    if (c.getVisibility() == CampaignVisibility.PRIVATE) return false;
                    if (c.getVisibility() == CampaignVisibility.SHARED)
                        return campaignAccessRepository
                                .findByCampaign_IdAndClub_Id(c.getId(), clubId).isPresent();
                    return false;
                })
                .map(c -> {
                    refreshDerivedFields(c);
                    boolean canAdd = false;
                    if (c.getVisibility() == CampaignVisibility.PUBLIC) {
                        canAdd = true;
                    } else if (c.getVisibility() == CampaignVisibility.SHARED) {
                        canAdd = campaignAccessRepository
                                .findByCampaign_IdAndClub_Id(c.getId(), clubId)
                                .map(this::hasAddEventPermission)
                                .orElse(false);
                    }
                    c.setCanAddEvent(canAdd);
                    return c;
                })
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS — logique métier dans le service, pas dans l'entité
    // ═══════════════════════════════════════════════════════════════════

    private void refreshDerivedFields(Campaign c) {
        if (c.getViews() == null)    c.setViews(0);
        if (c.getFeatured() == null) c.setFeatured(false);
        c.setStatus(computeStatus(c));

        int eventsCount       = (int) eventRepository.countByCampaignId(c.getId());
        Long participantsLong = participantRepository.countParticipantsByCampaignId(c.getId());
        int totalParticipants = participantsLong != null ? participantsLong.intValue() : 0;

        c.setEventsCount(eventsCount);
        c.setTotalParticipants(totalParticipants);
        c.setCurrentParticipants(totalParticipants);
    }

    private void syncCampaignCounts(Long campaignId) {
        campaignRepository.findById(campaignId).ifPresent(campaign -> {
            int eventsCount       = (int) eventRepository.countByCampaignId(campaignId);
            Long participantsLong = participantRepository.countParticipantsByCampaignId(campaignId);
            int totalParticipants = participantsLong != null ? participantsLong.intValue() : 0;

            campaign.setEventsCount(eventsCount);
            campaign.setTotalParticipants(totalParticipants);
            campaign.setCurrentParticipants(totalParticipants);
            campaignRepository.save(campaign);
        });
    }

    private boolean campaignHasParticipants(Long campaignId) {
        return eventRepository.findByCampaignId(campaignId).stream()
                .anyMatch(e -> e.getParticipantsCount() != null
                        && e.getParticipantsCount() > 0);
    }

    private void notifyEventManagersOfLock(Campaign campaign,
                                           int eventsCount,
                                           int totalParticipants) {
        log.info("[CAMPAIGN_LOCK] '{}' (ID:{}) verrouillée — {} event(s), {} participant(s).",
                campaign.getTitle(), campaign.getId(), eventsCount, totalParticipants);

        if (campaign.getOwnerClub() == null) return;

        List<User> managers = userRepository.findByClubIdAndRole(
                campaign.getOwnerClub().getId(), RoleType.EVENT_MANAGER);

        if (managers == null || managers.isEmpty()) {
            log.info("[CAMPAIGN_LOCK] Aucun manager pour le club: {}",
                    campaign.getOwnerClub().getName());
            return;
        }

        for (User manager : managers) {
            if (manager.getEmail() == null || manager.getEmail().isBlank()) continue;
            try {
                SimpleMailMessage email = new SimpleMailMessage();
                email.setTo(manager.getEmail());
                email.setFrom(fromAddress);
                email.setSubject("[Cluverse] Campagne verrouillée : " + campaign.getTitle());
                email.setText(
                        "Bonjour " + manager.getFirstName() + ",\n\n"
                                + "La campagne \"" + campaign.getTitle() + "\" a été verrouillée "
                                + "car elle contient des événements avec des participants.\n\n"
                                + "Détails :\n"
                                + "- Événements : " + eventsCount + "\n"
                                + "- Participants : " + totalParticipants + "\n\n"
                                + "Cette campagne ne peut plus être supprimée.\n\n"
                                + "Cordialement,\nL'équipe Cluverse"
                );
                mailSender.send(email);
                log.info("[EMAIL] Envoyé à : {}", manager.getEmail());
            } catch (Exception e) {
                log.error("[EMAIL] Échec pour {} : {}", manager.getEmail(), e.getMessage());
            }
        }
    }

    private void notifyEventManagersOfCancellation(Campaign campaign, List<Event> linkedEvents) {
        log.info("[CAMPAIGN_CANCEL] Starting notifications for campaign '{}' (ID:{}) with {} event(s)",
                campaign.getTitle(), campaign.getId(), linkedEvents != null ? linkedEvents.size() : 0);

        if (campaign.getOwnerClub() == null) {
            log.warn("[CAMPAIGN_CANCEL] Campaign {} has no owner club - cannot notify", campaign.getId());
            return;
        }

        if (linkedEvents == null || linkedEvents.isEmpty()) {
            log.warn("[CAMPAIGN_CANCEL] Campaign {} has no linked events - skipping notifications", campaign.getId());
            return;
        }

        log.info("[CAMPAIGN_CANCEL] Processing {} linked events for campaign '{}'", linkedEvents.size(), campaign.getTitle());

        // Collect clubs from all linked events
        Set<Long> clubIds = new java.util.HashSet<>();
        for (Event event : linkedEvents) {
            if (event.getClub() != null && event.getClub().getId() != null) {
                log.debug("[CAMPAIGN_CANCEL] Event '{}' belongs to club {}", event.getTitle(), event.getClub().getId());
                clubIds.add(event.getClub().getId());
            } else {
                log.warn("[CAMPAIGN_CANCEL] Event '{}' (ID:{}) has no club associated", event.getTitle(), event.getId());
            }
        }

        log.info("[CAMPAIGN_CANCEL] Found {} unique clubs from linked events", clubIds.size());

        if (clubIds.isEmpty()) {
            log.warn("[CAMPAIGN_CANCEL] No clubs found for any linked events");
            return;
        }

        // Get all event managers from the linked clubs
        Set<Long> managerIds = new java.util.HashSet<>();
        for (Long clubId : clubIds) {
            log.debug("[CAMPAIGN_CANCEL] Searching for EVENT_MANAGER users in club {}", clubId);
            List<User> clubManagers = userRepository.findByClubIdAndRole(
                    clubId, RoleType.EVENT_MANAGER);            log.info("[CAMPAIGN_CANCEL] Found {} EVENT_MANAGER(s) in club {}", clubManagers != null ? clubManagers.size() : 0, clubId);
            
            if (clubManagers != null && !clubManagers.isEmpty()) {
                for (User manager : clubManagers) {
                    if (manager.getId() != null) {
                        log.debug("[CAMPAIGN_CANCEL] Adding manager {} (email: {}) from club {}", 
                                manager.getId(), manager.getEmail(), clubId);
                        managerIds.add(manager.getId());
                    }
                }
            }
        }

        log.info("[CAMPAIGN_CANCEL] Total unique EVENT_MANAGERs collected: {}", managerIds.size());

        if (managerIds.isEmpty()) {
            log.warn("[CAMPAIGN_CANCEL] No EVENT_MANAGER users found in any linked club");
            return;
        }

        // Fetch all managers
        List<User> managers = userRepository.findAllById(managerIds);
        log.info("[CAMPAIGN_CANCEL] Retrieved {} managers from database", managers.size());
        
        int sentCount = 0;
        int failedCount = 0;
        
        for (User manager : managers) {
            if (manager.getEmail() == null || manager.getEmail().isBlank()) {
                log.warn("[CAMPAIGN_CANCEL] Manager {} ({} {}) has no email address", 
                        manager.getId(), manager.getFirstName(), manager.getLastName());
                failedCount++;
                continue;
            }

            try {
                log.debug("[CAMPAIGN_CANCEL] Preparing email for manager {} ({})", manager.getId(), manager.getEmail());
                
                SimpleMailMessage email = new SimpleMailMessage();
                email.setTo(manager.getEmail());
                email.setFrom(fromAddress);
                email.setSubject("[Cluverse] Campagne annulée : " + campaign.getTitle());
                email.setText(
                        "Bonjour " + manager.getFirstName() + ",\n\n"
                                + "La campagne \"" + campaign.getTitle() + "\" a été annulée.\n\n"
                                + "Détails :\n"
                                + "- Club propriétaire : " + campaign.getOwnerClub().getName() + "\n"
                                + "- Événements liés : " + linkedEvents.size() + "\n\n"
                                + "Cette annulation affecte tous les événements associés à la campagne.\n"
                                + "Veuillez vérifier le statut de vos événements.\n\n"
                                + "Cordialement,\nL'équipe Cluverse"
                );
                
                log.debug("[CAMPAIGN_CANCEL] Sending email via JavaMailSender to {}", manager.getEmail());
                mailSender.send(email);
                log.info("[EMAIL_CANCEL] ✓ Email successfully sent to: {} ({})", 
                        manager.getEmail(), manager.getFirstName() + " " + manager.getLastName());
                sentCount++;
            } catch (Exception e) {
                log.error("[EMAIL_CANCEL] ✗ Failed to send email to {} (ID: {}): {} | Exception: {}", 
                        manager.getEmail(), manager.getId(), e.getMessage(), e.getClass().getSimpleName());
                log.debug("[EMAIL_CANCEL] Full stack trace:", e);
                failedCount++;
            }
        }

        log.info("[CAMPAIGN_CANCEL] Email notification summary: {} sent, {} failed", sentCount, failedCount);
    }
        

    private CampaignStatus computeStatus(Campaign c) {
        if (c.getStatus() == CampaignStatus.CANCELLED
                || c.getStatus() == CampaignStatus.LOCKED
                || c.getStatus() == CampaignStatus.DISABLED
                || c.getStatus() == CampaignStatus.ARCHIVED) {
            return c.getStatus();
        }
        LocalDateTime now = LocalDateTime.now();
        if (c.getStartDate() == null || c.getEndDate() == null) return CampaignStatus.PLANNED;
        if (now.isBefore(c.getStartDate()))  return CampaignStatus.PLANNED;
        if (now.isAfter(c.getEndDate()))     return CampaignStatus.FINISHED;
        return CampaignStatus.ACTIVE;
    }

    private void validateDates(Campaign c) {
        if (c.getStartDate() != null && c.getEndDate() != null
                && c.getEndDate().isBefore(c.getStartDate())) {
            throw new RuntimeException("Invalid dates");
        }
    }

    private void applyRequestToCampaign(Campaign c, CampaignRequest req) {
        if (req.getTitle()       != null) c.setTitle(req.getTitle());
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getStartDate()   != null) c.setStartDate(req.getStartDate());
        if (req.getEndDate()     != null) c.setEndDate(req.getEndDate());
        if (req.getFeatured()    != null) c.setFeatured(req.getFeatured());
        if (req.getMaxParticipants() != null) c.setMaxParticipants(req.getMaxParticipants());
    }

    private boolean hasViewPermission(CampaignAccess a) {
        return a.getPermissions().contains(CampaignPermission.VIEW)
                || a.getPermissions().contains(CampaignPermission.ADD_EVENT)
                || a.getPermissions().contains(CampaignPermission.MANAGE);
    }

    private boolean hasAddEventPermission(CampaignAccess a) {
        return a.getPermissions().contains(CampaignPermission.ADD_EVENT)
                || a.getPermissions().contains(CampaignPermission.MANAGE);
    }

    private boolean canSeeForTop(Campaign c, Long clubId, List<CampaignAccess> accesses) {
        if (c == null) return false;
        if (c.getOwnerClub() != null && c.getOwnerClub().getId().equals(clubId)) return true;
        if (c.getVisibility() == CampaignVisibility.PUBLIC) return true;
        if (c.getVisibility() == CampaignVisibility.PRIVATE)
            return c.getOwnerClub() != null && c.getOwnerClub().getId().equals(clubId);
        if (c.getVisibility() == CampaignVisibility.SHARED)
            return accesses.stream()
                    .anyMatch(a -> a.getCampaign() != null
                            && a.getCampaign().getId().equals(c.getId()));
        return false;
    }

    private int compareCampaignRanking(Campaign c1, Campaign c2) {
        int events1 = c1.getEventsCount() != null ? c1.getEventsCount() : 0;
        int events2 = c2.getEventsCount() != null ? c2.getEventsCount() : 0;
        if (events1 != events2) return Integer.compare(events2, events1);

        int participants1 = c1.getTotalParticipants() != null ? c1.getTotalParticipants() : 0;
        int participants2 = c2.getTotalParticipants() != null ? c2.getTotalParticipants() : 0;
        if (participants1 != participants2) return Integer.compare(participants2, participants1);

        int views1 = c1.getViews() != null ? c1.getViews() : 0;
        int views2 = c2.getViews() != null ? c2.getViews() : 0;
        if (views1 != views2) return Integer.compare(views2, views1);

        Long id1 = c1.getId() != null ? c1.getId() : Long.MAX_VALUE;
        Long id2 = c2.getId() != null ? c2.getId() : Long.MAX_VALUE;
        return Long.compare(id1, id2);
    }
}
