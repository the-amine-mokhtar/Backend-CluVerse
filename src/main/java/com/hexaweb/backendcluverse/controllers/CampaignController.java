package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.CampaignRequest;
import com.hexaweb.backendcluverse.entities.event.Campaign;
import com.hexaweb.backendcluverse.entities.event.CampaignAccess;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.enumerations.CampaignPermission;
import com.hexaweb.backendcluverse.enumerations.CampaignStatus;
import com.hexaweb.backendcluverse.services.CampaignService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService service;
    private final JwtUtil jwt;

    public CampaignController(CampaignService service, JwtUtil jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    private String resolve(String header) {
        return jwt.resolveBearerToken(header);
    }

    private Long clubId(String token) {
        return jwt.extractClubId(token);
    }

    private Long userId(String token) {
        return jwt.extractUserId(token);
    }

    private boolean admin(String token) {
        return Boolean.TRUE.equals(jwt.extractIsSuperAdmin(token))
                || "PRESIDENT".equals(jwt.extractRole(token));
    }

    @GetMapping
    public List<Campaign> all(@RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.getAllCampaigns(clubId(token), admin(token));
    }

    @GetMapping("/{id}")
    public Campaign one(@PathVariable Long id,
                        @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        Long uid = userId(token);
        service.getCampaignById(id, clubId(token), admin(token));
        if (uid != null) {
            return service.recordCampaignView(id, uid);
        }
        return service.getCampaignById(id, clubId(token), admin(token));
    }

    @PostMapping
    public Campaign create(@ModelAttribute CampaignRequest req,
                           @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.createCampaign(req, clubId(token), admin(token));
    }

    @PutMapping("/{id}")
    public Campaign update(@PathVariable Long id,
                           @ModelAttribute CampaignRequest req,
                           @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.updateCampaign(id, req, clubId(token), admin(token));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id,
                                                      @RequestHeader("Authorization") String header) {
        String token = resolve(header);

        try {
            Map<String, Object> result = service.deleteCampaign(id, clubId(token), admin(token));
            String action = (String) result.get("action");

            return switch (action) {
                case "DELETED", "ARCHIVED" -> ResponseEntity.ok(result);
                case "LOCKED" -> ResponseEntity.status(423).body(result);
                default -> ResponseEntity.ok(result);
            };
        } catch (RuntimeException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("action", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelCampaign(@PathVariable Long id,
                                                   @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        try {
            Campaign result = service.cancelCampaign(id, clubId(token), admin(token));
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("action", "ERROR");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/{id}/events/{eventId}")
    public Event addEvent(@PathVariable Long id,
                          @PathVariable Long eventId,
                          @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.assignEventToCampaign(id, eventId, clubId(token), admin(token));
    }

    @DeleteMapping("/{id}/events/{eventId}")
    public Event removeEvent(@PathVariable Long id,
                             @PathVariable Long eventId,
                             @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.removeEventFromCampaign(id, eventId, clubId(token), admin(token));
    }

    @GetMapping("/accessible")
    public List<Campaign> accessibleForMyClub(@RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.getCampaignsSharedWithClub(clubId(token));
    }

    @GetMapping("/top-5")
    public List<Campaign> getTop5Campaigns(@RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.getTop5CampaignsForEventManager(clubId(token), admin(token));
    }

    @GetMapping("/for-event-form")
    public List<Campaign> getCampaignsForEventForm(@RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.getCampaignsForEventForm(clubId(token), admin(token));
    }

    @PostMapping("/{campaignId}/permissions/{clubId}")
    public CampaignAccess grantPermission(@PathVariable Long campaignId,
                                          @PathVariable Long clubId,
                                          @RequestParam CampaignPermission permission,
                                          @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.grantPermission(campaignId, clubId, permission, this.clubId(token), admin(token));
    }

    @DeleteMapping("/{campaignId}/permissions/{clubId}")
    public ResponseEntity<Void> revokePermission(@PathVariable Long campaignId,
                                                 @PathVariable Long clubId,
                                                 @RequestParam CampaignPermission permission,
                                                 @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        service.revokePermission(campaignId, clubId, permission, this.clubId(token), admin(token));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{campaignId}/permissions")
    public List<CampaignAccess> getPermissions(@PathVariable Long campaignId,
                                               @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.getCampaignAccesses(campaignId, clubId(token), admin(token));
    }

    @GetMapping("/{id}/participants")
    public List<?> participants(@PathVariable Long id,
                                @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.getParticipants(id, clubId(token), admin(token));
    }

    @GetMapping("/debug/status")
    public Map<String, Object> getDebugStatus(@RequestHeader("Authorization") String header) {
        String token = resolve(header);
        if (!admin(token)) {
            throw new RuntimeException("Unauthorized");
        }

        Long cid = clubId(token);
        Map<String, Object> debug = new HashMap<>();
        debug.put("yourClubId", cid);
        debug.put("totalCampaigns", service.getAllCampaigns(cid, admin(token)).size());
        debug.put("accessibleCampaigns", service.getCampaignsSharedWithClub(cid).size());
        debug.put("top5Campaigns", service.getTop5CampaignsForEventManager(cid, admin(token)).size());
        return debug;
    }

    @PutMapping("/{id}/status")
    public Campaign updateStatus(@PathVariable Long id,
                                 @RequestParam CampaignStatus status,
                                 @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.updateCampaignStatus(id, status, clubId(token), admin(token));
    }

    @GetMapping("/{id}/events")
    public List<Event> getCampaignEvents(@PathVariable Long id,
                                         @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        return service.getCampaignEvents(id, clubId(token), admin(token));
    }

    @PostMapping("/{id}/views")
    public Campaign recordView(@PathVariable Long id,
                               @RequestHeader("Authorization") String header) {
        String token = resolve(header);
        Long uid = userId(token);
        service.getCampaignById(id, clubId(token), admin(token));
        return service.recordCampaignView(id, uid);
    }

}
