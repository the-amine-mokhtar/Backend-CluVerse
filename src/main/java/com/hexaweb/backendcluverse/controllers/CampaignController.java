package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.CampaignRequest;
import com.hexaweb.backendcluverse.entities.event.Campaign;
import com.hexaweb.backendcluverse.entities.event.CampaignAccess;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.enumerations.CampaignPermission;
import com.hexaweb.backendcluverse.services.CampaignService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService service;
    private final JwtUtil jwt;

    private Long clubId(String token) {
        return jwt.extractClubId(token);
    }

    private Long userId(String token) {
        return jwt.extractUserId(token);
    }

   

    private String resolve(String h) {
        return jwt.resolveBearerToken(h);
    }
private boolean admin(String token) {
    String role = jwt.extractRole(token);
    return Boolean.TRUE.equals(jwt.extractIsSuperAdmin(token))
        || "PRESIDENT".equals(role);
}
    // ✅ FIX — un seul @GetMapping (suppression du doublon qui causait une erreur de mapping)
    // Le backend filtre déjà selon visibilité, expiration et droits d'accès
    @GetMapping
    public List<Campaign> all(@RequestHeader("Authorization") String h) {
        String t = resolve(h);
        return service.getAllCampaigns(clubId(t), admin(t));
    }

    @GetMapping("/{id}")
public Campaign one(@PathVariable Long id,
                    @RequestHeader("Authorization") String h) {

    String t = resolve(h);
    Long uid = userId(t);

    // 🔥 enregistrer la vue sans remplacer la réponse
    if (uid != null) {
        service.recordCampaignView(id, uid);
    }

    return service.getCampaignById(id, clubId(t), admin(t));
}

    @PostMapping
    public Campaign create(@ModelAttribute CampaignRequest req,
                           @RequestHeader("Authorization") String h) {
        String t = resolve(h);
        return service.createCampaign(req, clubId(t), admin(t));
    }

    @PutMapping("/{id}")
    public Campaign update(@PathVariable Long id,
                           @ModelAttribute CampaignRequest req,
                           @RequestHeader("Authorization") String h) {
        String t = resolve(h);
        return service.updateCampaign(id, req, clubId(t), admin(t));
    }

 @DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id,
                                   @RequestHeader("Authorization") String h) {

    String t = resolve(h);

    try {
        service.deleteCampaign(id, clubId(t), admin(t));
        return ResponseEntity.ok().build();
    } catch (Exception e) {
        return ResponseEntity.badRequest().build();
    }
}

    @PostMapping("/{id}/events/{eventId}")
    public Event addEvent(@PathVariable Long id,
                          @PathVariable Long eventId,
                          @RequestHeader("Authorization") String h) {
        String t = resolve(h);
        return service.assignEventToCampaign(id, eventId, clubId(t), admin(t));
    }

    @DeleteMapping("/{id}/events/{eventId}")
    public Event removeEvent(@PathVariable Long id,
                             @PathVariable Long eventId,
                             @RequestHeader("Authorization") String h) {
        String t = resolve(h);
        return service.removeEventFromCampaign(id, eventId, clubId(t), admin(t));
    }
        /**
 * Campagnes accessibles pour le club connecté via CampaignAccess.
 * Accessible par tous les rôles (EVENT_MANAGER, PRESIDENT, etc.)
 */
@GetMapping("/accessible")
public List<Campaign> accessibleForMyClub(
        @RequestHeader("Authorization") String h) {
    String t = resolve(h);
    return service.getCampaignsSharedWithClub(clubId(t));
}

/**
 * TOP 5 Campaigns pour dashboard EVENT_MANAGER
 * Inclut : PUBLIC + SHARED avec vous + PRIVATE de votre club
 * Exclus : expirées, CANCELLED, FINISHED
 * Trié par : featured, views, date
 */
@GetMapping("/top-5")
public List<Campaign> getTop5Campaigns(
        @RequestHeader("Authorization") String h) {
    String t = resolve(h);
    return service.getTop5CampaignsForEventManager(clubId(t), admin(t));
}

/**
 * Campaigns pour formulaire création/modification EVENT
 * Même logique que top-5 (sans limite) pour filtrer et afficher en dropdown
 */
@GetMapping("/for-event-form")
public List<Campaign> getCampaignsForEventForm(
        @RequestHeader("Authorization") String h) {
    String t = resolve(h);
    return service.getCampaignsForEventForm(clubId(t), admin(t));
}
@PostMapping("/{campaignId}/permissions/{clubId}")
public CampaignAccess grantPermission(
        @PathVariable Long campaignId,
        @PathVariable Long clubId,
        @RequestParam CampaignPermission permission,
        @RequestHeader("Authorization") String h) {

    String t = resolve(h);

    return service.grantPermission(
            campaignId,
            clubId,
            permission,
            this.clubId(t),
            admin(t)
    );
}
@GetMapping("/{campaignId}/permissions")
public List<CampaignAccess> getPermissions(
        @PathVariable Long campaignId,
        @RequestHeader("Authorization") String h) {

    String t = resolve(h);

    return service.getCampaignAccesses(
            campaignId,
            clubId(t),
            admin(t)
    );
}

/**
 * DEBUG — Affiche le diagnostic pourquoi rien n'apparaît
 * Montre : total campaigns, total accessible, vos clubs, etc.
 */
@GetMapping("/debug/status")
public Map<String, Object> getDebugStatus(
        @RequestHeader("Authorization") String h) {

    String t = resolve(h);

    if (!admin(t)) {
        throw new RuntimeException("Unauthorized");
    }

    Long cid = clubId(t);

    Map<String, Object> debug = new java.util.HashMap<>();
    debug.put("yourClubId", cid);
    debug.put("totalCampaigns", service.getAllCampaigns(cid, admin(t)).size());
    debug.put("accessibleCampaigns", service.getCampaignsSharedWithClub(cid).size());
    debug.put("top5Campaigns", service.getTop5CampaignsForEventManager(cid, admin(t)).size());

    return debug;
}
@GetMapping("/{id}/participants")
public List<?> participants(@PathVariable Long id,
                           @RequestHeader("Authorization") String h) {
    String t = resolve(h);
    return service.getParticipants(id, clubId(t), admin(t));
}}