package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.EventRequest;
import com.hexaweb.backendcluverse.dto.EventAiDashboardDto;
import com.hexaweb.backendcluverse.dto.EventAiSchedulingDto;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import com.hexaweb.backendcluverse.services.EventAiService;
import com.hexaweb.backendcluverse.services.EventService;
import com.hexaweb.backendcluverse.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Validated
public class EventController {

    private final EventService     eventService;
    private final EventAiService   eventAiService;
    private final JwtUtil          jwtUtil;
    private final EventRepository  eventRepository;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    // ═══════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping
    public Event create(@Valid @RequestBody EventRequest req,
                        @RequestHeader("Authorization") String auth) {
        Long clubId = extractClubId(auth);
        Event created = eventService.createEvent(req, clubId);
        // Recharger avec la location jointurée
        return eventRepository.findByIdWithLocation(created.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Event created but could not be retrieved"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE — ✅ FIX : retourne 200 + l'event complet avec location, gère les erreurs métier
    // ═══════════════════════════════════════════════════════════════════════

    @PutMapping("/{id}")
    public Event update(@PathVariable Long id,
                        @Valid @RequestBody EventRequest req,
                        @RequestHeader("Authorization") String auth) {
        Long clubId = extractClubId(auth);
        try {
            Event updated = eventService.updateEvent(id, req, clubId);
            // Recharger avec la location jointurée
            return eventRepository.findByIdWithLocation(updated.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found after update"));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            if (msg.startsWith("Not allowed"))    throw new ResponseStatusException(HttpStatus.FORBIDDEN,  msg);
            if (msg.startsWith("Event not found")) throw new ResponseStatusException(HttpStatus.NOT_FOUND,  msg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET BY ID
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    public Event getById(@PathVariable Long id,
                         @RequestHeader("Authorization") String auth) {
        extractClubId(auth); // vérification du token
        return eventRepository.findByIdWithLocation(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + id));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET MY CLUB
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/my-club")
    public List<Event> myEvents(@RequestHeader("Authorization") String auth) {
        Long clubId = extractClubId(auth);
        return eventService.findByClubId(clubId);
    }

    @GetMapping("/my-club/ai-dashboard")
    public EventAiDashboardDto getAiDashboard(@RequestHeader("Authorization") String auth) {
        Long clubId = extractClubId(auth);
        return eventAiService.buildOrganizerDashboard(clubId);
    }

    @GetMapping("/my-club/ai-scheduling")
    public EventAiSchedulingDto getAiScheduling(@RequestHeader("Authorization") String auth) {
        Long clubId = extractClubId(auth);
        return eventAiService.buildOrganizerSchedulingOptimizer(clubId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════
    // GET ALL (public / admin)
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/all")
    public List<Event> getAllPublic(@RequestHeader("Authorization") String auth) {
        Long clubId = extractClubId(auth);
        return eventService.getAllAccessibleEvents(clubId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE — ✅ FIX : 404 si not found
    // ═══════════════════════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestHeader("Authorization") String auth) {
        extractClubId(auth);
        try {
            eventService.deleteEvent(id);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCEL (Set status to CANCELLED + notify participants via SMS)
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/cancel")
    public Event cancelEvent(@PathVariable Long id,
                             @RequestHeader("Authorization") String auth) {
        Long clubId = extractClubId(auth);
        try {
            Event cancelled = eventService.cancelEvent(id, clubId);
            return eventRepository.findByIdWithLocation(cancelled.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found after cancellation"));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            if (msg.startsWith("Not allowed"))    throw new ResponseStatusException(HttpStatus.FORBIDDEN,  msg);
            if (msg.startsWith("Event not found")) throw new ResponseStatusException(HttpStatus.NOT_FOUND,  msg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RECOUNT PARTICIPANTS
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/recount")
    public void recount(@PathVariable Long id) {
        eventService.updateParticipantsCount(id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPLOAD IMAGE — ✅ retourne JSON { url }
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/upload-image")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Path dir = Paths.get(uploadDir, "events");
            Files.createDirectories(dir);
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Files.write(dir.resolve(filename), file.getBytes());
            String url = "/uploads/events/" + filename;
            return Map.of("url", "http://localhost:8081" + url);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Upload failed: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/debug/my-club")
    public Map<String, Object> debugMyEvents(@RequestHeader("Authorization") String auth) {
        try {
            String token  = jwtUtil.resolveBearerToken(auth);
            Long   clubId = jwtUtil.extractClubId(token);
            List<Event> myEvents = clubId != null ? eventService.findByClubId(clubId) : List.of();
            return Map.of(
                    "clubId",           clubId != null ? clubId : "NULL",
                    "myEventsCount",    myEvents.size(),
                    "totalEventsCount", eventRepository.count()
            );
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════════════════

    private Long extractClubId(String auth) {
        String token  = jwtUtil.resolveBearerToken(auth);
        Long   clubId = jwtUtil.extractClubId(token);
        if (clubId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Club ID not found in token");
        }
        return clubId;
    }
}
