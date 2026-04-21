package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.EventRequest;
import com.hexaweb.backendcluverse.dto.EventResponseDTO;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.services.EventService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class EventController {
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    @GetMapping("/my-club")
    public List<EventResponseDTO> getMyEvents(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "status", required = false) String statusParam) {
        String token = resolveToken(authHeader);
        Long clubId = jwtUtil.extractClubId(token);

        List<EventResponseDTO> events = eventService.findByClubIdWithDetails(clubId);
        
        // Filtrer par status
        if (statusParam != null && !statusParam.isEmpty()) {
            try {
                EventStatus status = EventStatus.valueOf(statusParam.toUpperCase());
                events = events.stream()
                        .filter(e -> e.getStatus() == status)
                        .toList();
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + statusParam);
            }
        } else {
            // Par défaut, retourner tous les événements (sauf annulés) pour les statistiques
            events = events.stream()
                    .filter(e -> e.getStatus() != EventStatus.CANCELLED)
                    .toList();
        }
        
        return events;
    }

    /**
     * Obtenir un événement par ID avec les détails enrichis
     */
    @GetMapping("/{id}")
    public EventResponseDTO getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        resolveToken(authHeader);

        Event event = eventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        
        return eventService.convertToResponseDTO(event);
    }

    /**
     * Vérifier la disponibilité de l'événement (capacité)
     */
    @GetMapping("/{id}/capacity-check")
    public EventResponseDTO checkCapacity(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        resolveToken(authHeader);
        
        Event event = eventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        
        return eventService.convertToResponseDTO(event);
    }

    /**
     * Obtenir tous les événements du club (ancien endpoint, compatible)
     */
    @GetMapping("/my-club-events")
    public List<Event> getMyClubEvents(@RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long clubId = jwtUtil.extractClubId(token);

        return eventRepository.findByClubId(clubId).stream()
                .filter(e -> e.getStatus() != EventStatus.CANCELLED)
                .filter(e -> e.getEndDate() == null || !e.getEndDate().isBefore(LocalDateTime.now()))
                .toList();
    }
    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String uploadDir = "uploads/";

            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, file.getBytes());

            return "http://localhost:8081/uploads/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Upload failed");
        }
    }
    // 🔹 CREATE
    @PostMapping
    public EventResponseDTO create(
            @Valid @RequestBody EventRequest req,
            BindingResult bindingResult,
            @RequestHeader("Authorization") String authHeader) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        String token = resolveToken(authHeader);
        checkManager(token);

        Long clubId = jwtUtil.extractClubId(token);
        Event event = eventService.createEvent(req, clubId);
        return eventService.convertToResponseDTO(event);
    }

    // 🔹 UPDATE
    @PutMapping("/{id}")
    public EventResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest req,
            BindingResult bindingResult,
            @RequestHeader("Authorization") String authHeader) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        String token = resolveToken(authHeader);
        checkManager(token);

        Event updatedEvent = eventService.updateEvent(id, req);
        return eventService.convertToResponseDTO(updatedEvent);
    }

    // 🔹 DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestHeader("Authorization") String authHeader) {

        String token = resolveToken(authHeader);
        checkManager(token);

        eventService.deleteEvent(id);
    }

    // 🔐 UTILS
    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private void checkManager(String token) {
        String role = jwtUtil.extractRole(token);
        if (!"EVENT_MANAGER".equals(role) && !"PRESIDENT".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
    @GetMapping("/stats")
    public List<Object[]> stats(@RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long clubId = jwtUtil.extractClubId(token);
        return eventService.getStatsByMonth(clubId);
    }
    @GetMapping("/club-events/cancelled")
    public List<Event> getCancelledEventsForClub(
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long clubId = jwtUtil.extractClubId(token);
        return eventService.findCancelledEventsByClubId(clubId);
    }

    /**
     * 🔹 GET participants for a specific event
     */
    @GetMapping("/{id}/participants")
    public List<EventParticipant> getEventParticipants(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        
        Event event = eventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        
        return event.getParticipants();
    }

    @PostMapping("/cancel-event/{id}")
    public void cancelEvent(@PathVariable Long id) {
        eventService.cancelEvent(id);
    }
}