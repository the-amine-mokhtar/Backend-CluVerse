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
import com.hexaweb.backendcluverse.repositories.CampaignAccessRepository;
import com.hexaweb.backendcluverse.repositories.CampaignRepository;
import com.hexaweb.backendcluverse.repositories.CampaignViewRepository;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.EventParticipantRepository;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CampaignService extends EntityServiceImpl<Campaign, Long> {

    private final CampaignRepository campaignRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final CloudinaryService cloudinaryService;
    private final CampaignAccessRepository campaignAccessRepository;
    private final ClubRepository clubRepository;
    private final CampaignViewRepository campaignViewRepository;
    private final UserRepository userRepository;

    public CampaignService(CampaignRepository repository,
                           EventRepository eventRepository,
                           EventParticipantRepository participantRepository,
                           CloudinaryService cloudinaryService,
                           CampaignAccessRepository campaignAccessRepository,
                           ClubRepository clubRepository,
                           CampaignViewRepository campaignViewRepository,
                           UserRepository userRepository) {
        super(repository);
        this.campaignRepository = repository;
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.cloudinaryService = cloudinaryService;
        this.campaignAccessRepository = campaignAccessRepository;
        this.clubRepository = clubRepository;
        this.campaignViewRepository = campaignViewRepository;
        this.userRepository = userRepository;
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET ALL — filtrer expirées + PRIVATE non autorisées + calculer canAddEvent
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

    boolean alreadyViewed =
            campaignViewRepository.existsByCampaignIdAndUserId(campaignId, userId);

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

    // ═══════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════
    public Campaign createCampaign(CampaignRequest req, Long clubId, boolean isSuperAdmin) {
        Club ownerClub = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found with id: " + clubId));

        Campaign campaign = new Campaign();

        if (req.getImageFile() != null && !req.getImageFile().isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadCampaignImage(req.getImageFile());
                campaign.setImageUrl(imageUrl);
            } catch (Exception e) {
                throw new RuntimeException("Image upload failed: " + e.getMessage());
            }
        }

        campaign.setOwnerClub(ownerClub);
        campaign.setVisibility(
                req.getVisibility() != null ? req.getVisibility() : CampaignVisibility.SHARED
        );

        applyRequestToCampaign(campaign, req);
        validateDates(campaign);
        refreshDerivedFields(campaign);

        return campaignRepository.save(campaign);
    }

    // ═══════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════
    public Campaign updateCampaign(Long id, CampaignRequest req, Long clubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        if (!isSuperAdmin &&
                (campaign.getOwnerClub() == null ||
                        !campaign.getOwnerClub().getId().equals(clubId))) {
            throw new RuntimeException("You can only update your own campaigns");
        }

        if (req.getImageFile() != null && !req.getImageFile().isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadCampaignImage(req.getImageFile());
                campaign.setImageUrl(imageUrl);
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
    // GET EVENTS OF CAMPAIGN
    // ═══════════════════════════════════════════════════════════════════
       public List<Event> getCampaignEvents(Long campaignId) {
        return eventRepository.findByCampaignId(campaignId);
    }

   public Event assignEventToCampaign(Long campaignId, Long eventId, Long clubId, boolean isSuperAdmin) {

    Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));

    Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

    if (!canAddEventToCampaign(campaign, clubId, isSuperAdmin)) {
        throw new RuntimeException("No permission to add event to campaign");
    }

    event.setCampaign(campaign);
    return eventRepository.save(event);
}

    // ═══════════════════════════════════════════════════════════════════
    // ASSIGN EVENT TO CAMPAIGN
    // ═══════════════════════════════════════════════════════════════════
    

    // ═══════════════════════════════════════════════════════════════════
    // REMOVE EVENT FROM CAMPAIGN
    // ═══════════════════════════════════════════════════════════════════
  public Event removeEventFromCampaign(Long campaignId, Long eventId, Long clubId, boolean isSuperAdmin) {

    Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));

    Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Event not found"));

    if (event.getCampaign() == null ||
        !event.getCampaign().getId().equals(campaignId)) {
        throw new RuntimeException("Event not linked to this campaign");
    }

    if (!isSuperAdmin &&
        (event.getClub() == null ||
         !event.getClub().getId().equals(clubId))) {
        throw new RuntimeException("Not allowed");
    }

    event.setCampaign(null);

    Event saved = eventRepository.save(event);

    refreshDerivedFields(campaign);
    campaignRepository.save(campaign);

    return saved;
}
    // ═══════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════
    @Transactional
    public void deleteCampaign(Long id, Long clubId, boolean isSuperAdmin) {

        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        // permission check
        if (!isSuperAdmin &&
                (campaign.getOwnerClub() == null ||
                        !campaign.getOwnerClub().getId().equals(clubId))) {
            throw new RuntimeException("Not allowed");
        }

        // 1. remove events
        List<Event> events = eventRepository.findByCampaignId(id);
        for (Event e : events) {
            e.setCampaign(null);
            eventRepository.save(e);
        }

        // 2. DELETE ACCESS PERMISSIONS FIRST (IMPORTANT FIX)
        List<CampaignAccess> accesses = campaignAccessRepository.findByCampaignId(id);

        for (CampaignAccess access : accesses) {
            access.getPermissions().clear();   // 🔥 IMPORTANT
            campaignAccessRepository.save(access);
        }

        campaignAccessRepository.deleteAll(accesses);

        // 3. delete views
        campaignViewRepository.deleteAllByCampaignId(id);

        // 4. delete campaign
        campaignRepository.delete(campaign);
    }

    // ═══════════════════════════════════════════════════════════════════
    // VISIBILITÉ — règles d'accès (avec support EVENT_MANAGER pour PRIVATE)
    // PUBLIC  → tous les clubs voient
    // SHARED  → seulement les clubs avec permission VIEW/ADD_EVENT/MANAGE
    // PRIVATE → club owner + EVENT_MANAGER du club propriétaire
    // ═══════════════════════════════════════════════════════════════════
  public boolean canViewCampaign(Long clubId, Campaign c, boolean isSuperAdmin) {

    if (isSuperAdmin) return true;

    if (c == null) return false;

    // owner peut toujours voir
    if (c.getOwnerClub() != null &&
        c.getOwnerClub().getId().equals(clubId)) {
        return true;
    }

    // PUBLIC visible pour tous
    if (c.getVisibility() == CampaignVisibility.PUBLIC) {
        return true;
    }

    // PRIVATE seulement owner
    if (c.getVisibility() == CampaignVisibility.PRIVATE) {
        return false;
    }

    // SHARED → via permissions
    return campaignAccessRepository
            .findByCampaignIdAndClubId(c.getId(), clubId)
            .map(this::hasViewPermission)
            .orElse(false);
}

  


    // ═══════════════════════════════════════════════════════════════════
    // PEUT AJOUTER UN EVENT
    // PUBLIC  → tous les event managers peuvent ajouter
    // SHARED  → seulement les clubs avec permission ADD_EVENT ou MANAGE
    // PRIVATE → club owner + EVENT_MANAGER du club propriétaire
    // ═══════════════════════════════════════════════════════════════════
 public boolean canAddEventToCampaign(Campaign campaign, Long clubId, boolean isSuperAdmin) {

    if (isSuperAdmin) return true;

    if (campaign == null) return false;

    // ✅ Owner du club = toujours autorisé
    if (campaign.getOwnerClub() != null &&
        campaign.getOwnerClub().getId().equals(clubId)) {
        return true;
    }

    // ✅ PUBLIC = tout le monde peut ajouter
    if (campaign.getVisibility() == CampaignVisibility.PUBLIC) {
        return true;
    }

    // ❌ PRIVATE = uniquement owner (déjà traité)
    if (campaign.getVisibility() == CampaignVisibility.PRIVATE) {
        return false;
    }

    // 🟡 SHARED = dépend des permissions
    return campaignAccessRepository
            .findByCampaignIdAndClubId(campaign.getId(), clubId)
            .map(this::hasAddEventPermission)
            .orElse(false);
}

    // ═══════════════════════════════════════════════════════════════════
    // PERMISSIONS MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════
    public CampaignAccess grantPermission(Long campaignId, Long clubId, CampaignPermission permission, Long ownerClubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!isSuperAdmin && (campaign.getOwnerClub() == null || !campaign.getOwnerClub().getId().equals(ownerClubId))) {
            throw new RuntimeException("Only campaign owner can manage permissions");
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));

        CampaignAccess access = campaignAccessRepository.findByCampaignIdAndClubId(campaignId, clubId)
                .orElse(new CampaignAccess(null, campaign, club, new java.util.HashSet<>()));

        access.getPermissions().add(permission);
        return campaignAccessRepository.save(access);
    }

    public void revokePermission(Long campaignId, Long clubId, CampaignPermission permission, Long ownerClubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!isSuperAdmin && (campaign.getOwnerClub() == null || !campaign.getOwnerClub().getId().equals(ownerClubId))) {
            throw new RuntimeException("Only campaign owner can manage permissions");
        }

        CampaignAccess access = campaignAccessRepository.findByCampaignIdAndClubId(campaignId, clubId)
                .orElseThrow(() -> new RuntimeException("Access not found"));

        access.getPermissions().remove(permission);

        if (access.getPermissions().isEmpty()) {
            campaignAccessRepository.delete(access);
        } else {
            campaignAccessRepository.save(access);
        }
    }

    public List<CampaignAccess> getCampaignAccesses(Long campaignId, Long ownerClubId, boolean isSuperAdmin) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!isSuperAdmin && (campaign.getOwnerClub() == null || !campaign.getOwnerClub().getId().equals(ownerClubId))) {
            throw new RuntimeException("Only campaign owner can view permissions");
        }

        return campaignAccessRepository.findByCampaignId(campaignId);
    }

    private void applyRequestToCampaign(Campaign c, CampaignRequest req) {
        if (req.getTitle() != null) c.setTitle(req.getTitle());
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getStartDate() != null) c.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) c.setEndDate(req.getEndDate());
    }

    private void refreshDerivedFields(Campaign c) {
        if (c.getViews() == null) c.setViews(0);
        if (c.getFeatured() == null) c.setFeatured(false);
        c.setStatus(computeStatus(c));
    }

    private CampaignStatus computeStatus(Campaign c) {
        LocalDateTime now = LocalDateTime.now();

        if (c.getStartDate() == null || c.getEndDate() == null) return CampaignStatus.PLANNED;
        if (now.isBefore(c.getStartDate())) return CampaignStatus.PLANNED;
        if (now.isAfter(c.getEndDate())) return CampaignStatus.FINISHED;
        return CampaignStatus.ACTIVE;
    }

    private void validateDates(Campaign c) {
        if (c.getStartDate() != null && c.getEndDate() != null &&
                c.getEndDate().isBefore(c.getStartDate())) {
            throw new RuntimeException("Invalid dates");
        }
    }

    private boolean hasViewPermission(CampaignAccess a) {
        return a.hasPermission(CampaignPermission.VIEW)
                || a.hasPermission(CampaignPermission.ADD_EVENT)
                || a.hasPermission(CampaignPermission.MANAGE);
    }

    private boolean hasAddEventPermission(CampaignAccess a) {
        return a.hasPermission(CampaignPermission.ADD_EVENT)
                || a.hasPermission(CampaignPermission.MANAGE);
    }


  

    // ═══════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════
   
private int computeCurrentParticipants(Long campaignId) {
    return participantRepository.countParticipantsByCampaignId(campaignId).intValue();
}
public List<?> getParticipants(Long campaignId, Long clubId, boolean isSuperAdmin) {

    Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new RuntimeException("Campaign not found"));

    // option sécurité
    if (!canViewCampaign(clubId, campaign, isSuperAdmin)) {
        throw new RuntimeException("Access denied");
    }

    return participantRepository.findByCampaignId(campaignId);
}

    // ═══════════════════════════════════════════════════════════════════
    // TOP 5 CAMPAIGNS POUR EVENT MANAGER / DASHBOARD
    // Campaigns : PUBLIC + SHARED avec vous + PRIVATE créées par votre président
    // Exclus : expirées, CANCELLED, FINISHED
    // Trié par : featured, views, dateStart DESC
    // ═══════════════════════════════════════════════════════════════════
    public List<Campaign> getTop5CampaignsForEventManager(Long clubId, boolean isSuperAdmin) {

        LocalDateTime now = LocalDateTime.now();

        // 🔥 preload des accès (évite N+1 queries)
        List<CampaignAccess> accesses =
                campaignAccessRepository.findByClubId(clubId);

        return campaignRepository.findAll().stream()

                // ❌ campaigns expirées
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(now))

                // ❌ status invalides
                .filter(c -> c.getStatus() != CampaignStatus.CANCELLED
                        && c.getStatus() != CampaignStatus.FINISHED)

                // 🔐 visibilité optimisée
                .filter(c -> canSeeForTop(c, clubId, accesses))

                .map(c -> {
                    refreshDerivedFields(c);
                    c.setCanAddEvent(canAddEventToCampaign(c, clubId, isSuperAdmin));
                    return c;
                })

                // 📊 SAFE SORT
                .sorted((c1, c2) -> {

                    boolean f1 = Boolean.TRUE.equals(c1.getFeatured());
                    boolean f2 = Boolean.TRUE.equals(c2.getFeatured());

                    if (f1 != f2) return f2 ? 1 : -1;

                    int v1 = c1.getViews() != null ? c1.getViews() : 0;
                    int v2 = c2.getViews() != null ? c2.getViews() : 0;

                    if (v1 != v2) return Integer.compare(v2, v1);

                    LocalDateTime d1 = c1.getStartDate() != null ? c1.getStartDate() : LocalDateTime.MIN;
                    LocalDateTime d2 = c2.getStartDate() != null ? c2.getStartDate() : LocalDateTime.MIN;

                    return d2.compareTo(d1);
                })

                .limit(5)
                .toList();
    }
    private boolean canSeeForTop(Campaign c, Long clubId, List<CampaignAccess> accesses) {

        if (c == null) return false;

        // PUBLIC → toujours visible
        if (c.getVisibility() == CampaignVisibility.PUBLIC) {
            return true;
        }

        // PRIVATE → seulement owner
        if (c.getVisibility() == CampaignVisibility.PRIVATE) {
            return c.getOwnerClub() != null
                    && c.getOwnerClub().getId().equals(clubId);
        }

        // SHARED → via accès préchargés (IMPORTANT FIX PERF)
        if (c.getVisibility() == CampaignVisibility.SHARED) {

            return accesses.stream()
                    .anyMatch(a ->
                            a.getCampaign() != null &&
                                    a.getCampaign().getId().equals(c.getId())
                    );
        }

        return false;
    }
    // ═══════════════════════════════════════════════════════════════════
    // CAMPAIGNS POUR FORMULAIRE CREATION/MODIFICATION EVENT
    // Même logique que Top 5 (sans limite) pour que user puisse choisir
    // ═══════════════════════════════════════════════════════════════════
    public List<Campaign> getCampaignsForEventForm(Long clubId, boolean isSuperAdmin) {
        LocalDateTime now = LocalDateTime.now();

        return campaignRepository.findAll().stream()
                // Exclure expirées
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(now))
                // Exclure cancelled/finished
                .filter(c -> c.getStatus() != CampaignStatus.CANCELLED
                          && c.getStatus() != CampaignStatus.FINISHED)
                // Filtrer par visibilité et accès
                .filter(c -> {
                    if (c.getVisibility() == CampaignVisibility.PUBLIC) return true;
                    if (c.getVisibility() == CampaignVisibility.SHARED) {
                        return campaignAccessRepository.findByCampaignIdAndClubId(c.getId(), clubId)
                                .isPresent();
                    }
                    if (c.getVisibility() == CampaignVisibility.PRIVATE) {
                        return c.getOwnerClub() != null && c.getOwnerClub().getId().equals(clubId);
                    }
                    return false;
                })
                .map(c -> {
                    refreshDerivedFields(c);
                    c.setCanAddEvent(canAddEventToCampaign(c, clubId, isSuperAdmin));
                    return c;
                })
                // Tri : startDate ASC (prochaines en premier)
                .sorted((c1, c2) -> {
                    LocalDateTime date1 = c1.getStartDate() != null ? c1.getStartDate() : LocalDateTime.MAX;
                    LocalDateTime date2 = c2.getStartDate() != null ? c2.getStartDate() : LocalDateTime.MAX;
                    return date1.compareTo(date2);
                })
                .collect(Collectors.toList());
    }
    /**
 * Retourne les campagnes accessibles pour un club donné via CampaignAccess.
 * + AUSSI les campaigns PUBLIC
 * - Exclut les campagnes owned par ce club (sauf PUBLIC)
 * - Exclut CANCELLED, FINISHED, expirées
 * - Calcule canAddEvent selon la permission réelle du club
 */
public List<Campaign> getCampaignsSharedWithClub(Long clubId) {
    LocalDateTime now = LocalDateTime.now();

    return campaignRepository.findAll().stream()
        // Exclure expirées
        .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(now))
        // Exclure CANCELLED/FINISHED
        .filter(c -> c.getStatus() != CampaignStatus.CANCELLED
                  && c.getStatus() != CampaignStatus.FINISHED)
        // Filtrer : PUBLIC + SHARED (via CampaignAccess) — excl. PRIVATE owned par ce club
        .filter(c -> {
            // 🟢 PUBLIC → Tous peuvent voir
            if (c.getVisibility() == CampaignVisibility.PUBLIC) return true;

            // 🔴 PRIVATE → Seulement le owner (excl. si c'est nous)
            if (c.getVisibility() == CampaignVisibility.PRIVATE) {
                if (c.getOwnerClub() != null && c.getOwnerClub().getId().equals(clubId)) {
                    return false; // Nos PRIVATE ne sont pas "shared"
                }
                return false;
            }

            // 🟡 SHARED → Seulement si CampaignAccess existe
            if (c.getVisibility() == CampaignVisibility.SHARED) {
                return campaignAccessRepository.findByCampaignIdAndClubId(c.getId(), clubId)
                        .isPresent();
            }

            return false;
        })
        .map(c -> {
            refreshDerivedFields(c);
            // canAddEvent = true seulement si permission ADD_EVENT ou MANAGE (pour SHARED)
            boolean canAdd = false;
            if (c.getVisibility() == CampaignVisibility.PUBLIC) {
                canAdd = true; // PUBLIC → can add
            } else if (c.getVisibility() == CampaignVisibility.SHARED) {
                canAdd = campaignAccessRepository
                    .findByCampaignIdAndClubId(c.getId(), clubId)
                    .map(this::hasAddEventPermission)
                    .orElse(false);
            }
            c.setCanAddEvent(canAdd);
            return c;
        })
        .collect(Collectors.toList());
}
}