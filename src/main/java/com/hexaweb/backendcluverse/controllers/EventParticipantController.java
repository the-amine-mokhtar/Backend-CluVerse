package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.services.EventParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/event-participants")
@RequiredArgsConstructor
public class EventParticipantController {

    private final EventParticipantService eventParticipantService;

    @GetMapping
    public List<EventParticipant> getAll() {
        return eventParticipantService.findAll();
    }

    @GetMapping("/{id}")
    public EventParticipant getById(@PathVariable Long id) {
        return eventParticipantService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public EventParticipant create(@RequestBody EventParticipant eventParticipant) {
        return eventParticipantService.save(eventParticipant);
    }

    @PutMapping("/{id}")
    public EventParticipant update(@PathVariable Long id, @RequestBody EventParticipant eventParticipant) {
        eventParticipant.setId(id);
        return eventParticipantService.save(eventParticipant);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        eventParticipantService.deleteById(id);
    }
}

