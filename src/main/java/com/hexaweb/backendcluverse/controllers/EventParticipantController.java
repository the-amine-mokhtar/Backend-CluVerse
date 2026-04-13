package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.EventParticipantRequest;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.repositories.EventParticipantRepository;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.services.EventParticipantService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class EventParticipantController {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventParticipantService participantService;
    private final JwtUtil jwtUtil;
    private final EventParticipantRepository eventParticipantRepository;

    // ─── GET tous les participants (admin) ────────────────────────────────────
    @GetMapping
    @Operation(summary = "Get all participants, optionally filtered by eventId")
    public List<EventParticipant> getParticipants(
            @RequestParam(required = false) Long eventId,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return eventId != null
                ? participantService.findByEventId(eventId)
                : participantService.findAll();
    }

    // ─── GET events du club de l'utilisateur connecté ─────────────────────────
    @GetMapping("/my-club-events")
    @Operation(summary = "Get all events of my club")
    public List<Event> getMyClubEvents(
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long clubId = jwtUtil.extractClubId(token);
        return eventRepository.findByClubId(clubId);
    }

    // ─── GET mes participations ────────────────────────────────────────────────
    @GetMapping("/my-events")
    @Operation(summary = "Get my own participations")
    public List<EventParticipant> getMyParticipations(
            @RequestHeader("Authorization") String authHeader) {

        String token = resolveToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);

        return participantService.findByUserId(userId);
    }
    // ─── GET par ID ────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Get one participation by ID")
    public EventParticipant getParticipantById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return participantService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation not found"));
    }

    // ─── POST participer ───────────────────────────────────────────────────────
    @PostMapping
    @Operation(summary = "Register to an event")
    public ResponseEntity<EventParticipant> addParticipant(
            @Valid @RequestBody EventParticipantRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        // ✅ On injecte l'userId depuis le token — le frontend n'a pas à l'envoyer
        Long userId = jwtUtil.extractUserId(token);
        request.setUserId(userId);
        EventParticipant saved = participantService.addParticipant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ─── PUT modifier ──────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @Operation(summary = "Update a participation")
    public ResponseEntity<EventParticipant> updateParticipant(
            @PathVariable Long id,
            @Valid @RequestBody EventParticipantRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        // ✅ Vérifier que la participation appartient bien à l'utilisateur
        participantService.checkOwnership(id, userId);
        return ResponseEntity.ok(participantService.updateParticipant(id, request));
    }

    // ─── DELETE annuler ────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel (delete) a participation")
    public void deleteParticipant(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        // ✅ Vérifier que la participation appartient bien à l'utilisateur
        participantService.checkOwnership(id, userId);
        participantService.deleteById(id);
    }
    // ─── Helper ────────────────────────────────────────────────────────────────
    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }
    @GetMapping("/my-events/cancelled")
    public List<EventParticipant> getMyCancelledParticipations(
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        return participantService.findCancelledByUserId(userId);
    }
    @PostMapping("/cancel-participation/{id}")
    public void cancelParticipation(@PathVariable Long id) {
        participantService.cancelParticipation(id);
    }

}
