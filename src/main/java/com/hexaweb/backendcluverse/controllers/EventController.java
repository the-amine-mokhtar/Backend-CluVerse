package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Event;
import com.hexaweb.backendcluverse.services.EventService;
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
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public List<Event> getAll() {
        return eventService.findAll();
    }

    @GetMapping("/{id}")
    public Event getById(@PathVariable Long id) {
        return eventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Event create(@RequestBody Event event) {
        return eventService.save(event);
    }

    @PutMapping("/{id}")
    public Event update(@PathVariable Long id, @RequestBody Event event) {
        event.setId(id);
        return eventService.save(event);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        eventService.deleteById(id);
    }
}

