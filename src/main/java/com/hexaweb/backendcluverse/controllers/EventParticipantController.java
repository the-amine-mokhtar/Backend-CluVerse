package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.EventParticipantRequest;
import com.hexaweb.backendcluverse.dto.ParticipantAiDashboardDto;
import com.hexaweb.backendcluverse.dto.WaitingListDto;
import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.services.EventAiService;
import com.hexaweb.backendcluverse.services.EventParticipantService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class EventParticipantController {

    private final EventParticipantService participantService;
    private final EventAiService          eventAiService;
    private final JwtUtil                 jwtUtil;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long resolveUserId(String auth) {
        return jwtUtil.extractUserId(jwtUtil.resolveBearerToken(auth));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ✅ NEW: UNIFIED PARTICIPATE — single endpoint for joining an event.
    //
    // The backend handles everything:
    //   - place available  → register + confirmation SMS → returns "REGISTERED"
    //   - event full       → waiting list + waiting SMS  → returns "WAITING_LIST_ADDED"
    //   - already joined   → 409 CONFLICT
    //
    // Frontend just calls this, reads the status, and shows the right UI.
    // No more requestParticipation → separate form → separate POST flow.
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/participate/{eventId}")
    public ResponseEntity<Map<String, String>> participate(
            @PathVariable Long eventId,
            @RequestBody EventParticipantRequest req,
            @RequestHeader("Authorization") String auth) {

        Long userId = resolveUserId(auth);
        String status = participantService.participate(eventId, userId, req);

        return ResponseEntity.ok(Map.of("status", status));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET ALL — by eventId OR current user's participations
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping
    public List<EventParticipant> getAll(
            @RequestParam(required = false) Long eventId,
            @RequestHeader("Authorization") String auth) {

        if (eventId != null) {
            return participantService.findByEventId(eventId);
        }
        return participantService.findByUserId(resolveUserId(auth));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET ONE
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    public EventParticipant getById(@PathVariable Long id) {
        return participantService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Participation not found"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE — direct (legacy, kept for compatibility)
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventParticipant create(@RequestBody EventParticipantRequest req,
                                   @RequestHeader("Authorization") String auth) {
        req.setUserId(resolveUserId(auth));
        return participantService.addParticipant(req);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @PutMapping("/{id}")
    public EventParticipant update(@PathVariable Long id,
                                   @RequestBody EventParticipantRequest req,
                                   @RequestHeader("Authorization") String auth) {
        participantService.checkOwnership(id, resolveUserId(auth));
        return participantService.updateParticipant(id, req);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestHeader("Authorization") String auth) {
        participantService.checkOwnership(id, resolveUserId(auth));
        participantService.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CANCEL
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/cancel/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id,
                       @RequestHeader("Authorization") String auth) {
        participantService.checkOwnership(id, resolveUserId(auth));
        participantService.cancelParticipation(id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REACTIVATE — re-register a cancelled participation
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/reactivate/{id}")
    public EventParticipant reactivate(@PathVariable Long id,
                                       @RequestHeader("Authorization") String auth) {
        participantService.checkOwnership(id, resolveUserId(auth));
        return participantService.reactivateParticipation(id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MY CANCELLED PARTICIPATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/me/cancelled")
    public List<EventParticipant> myCancelled(@RequestHeader("Authorization") String auth) {
        return participantService.findCancelledByUserId(resolveUserId(auth));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MY WAITING LIST
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/me/waiting-list")
    public List<WaitingListDto> myWaitingList(@RequestHeader("Authorization") String auth) {
        Long userId = resolveUserId(auth);
        return participantService.getWaitingListForUser(userId);
    }

    @GetMapping("/me/ai-dashboard")
    public ParticipantAiDashboardDto getParticipantAiDashboard(
            @RequestHeader("Authorization") String auth) {
        String token = jwtUtil.resolveBearerToken(auth);
        Long userId = jwtUtil.extractUserId(token);
        Long clubId = jwtUtil.extractClubId(token);
        return eventAiService.buildParticipantDashboard(userId, clubId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WAITING LIST — Step 1: check availability (legacy, kept for compatibility)
    // ═══════════════════════════════════════════════════════════════════════════



    // ═══════════════════════════════════════════════════════════════════════════
    // WAITING LIST — Step 2: join after user consent
    // ═══════════════════════════════════════════════════════════════════════════
    @PostMapping("/waiting-list/{eventId}")
    public ResponseEntity<String> joinWaitingList(
            @PathVariable Long eventId,
            @RequestParam boolean accept,
            @RequestHeader("Authorization") String auth) {

        String result = participantService.joinWaitingList(
                eventId,
                resolveUserId(auth),
                accept
        );

        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════════════
    // CONFIRM PROMOTION
    // ═══════════════════════════════════════

    @PostMapping("/confirm/{waitingId}")
    public ResponseEntity<String> confirmPromotion(
            @PathVariable Long waitingId) {

        String result = participantService.confirmPromotion(waitingId);
        return ResponseEntity.ok(result);
    }
}
