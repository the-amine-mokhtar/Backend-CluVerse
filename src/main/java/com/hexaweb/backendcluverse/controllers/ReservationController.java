package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.event.Reservation;
import com.hexaweb.backendcluverse.services.ReservationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public List<Reservation> getAll(@RequestParam(required = false) Long eventId) {
        if (eventId != null) {
            return reservationService.findByEventId(eventId);
        }
        return reservationService.findAll();
    }

    @GetMapping("/{id}")
    public Reservation getById(@PathVariable Long id) {
        return reservationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Reservation create(@RequestBody com.hexaweb.backendcluverse.dto.ReservationRequest request) {
        return reservationService.createReservation(request);
    }

    @PutMapping("/{id}")
    public Reservation update(@PathVariable Long id, @RequestBody com.hexaweb.backendcluverse.dto.ReservationRequest request) {
        return reservationService.updateReservation(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        reservationService.deleteById(id);
    }
}

