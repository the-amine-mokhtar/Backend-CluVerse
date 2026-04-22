package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.ReservationRequest;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.event.Reservation;
import com.hexaweb.backendcluverse.entities.logistics.Resource;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import com.hexaweb.backendcluverse.repositories.ReservationRepository;
import com.hexaweb.backendcluverse.repositories.ResourceRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ReservationService extends EntityServiceImpl<Reservation, Long> {

    private final EventRepository eventRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository repository,
                              EventRepository eventRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository) {
        super(repository);
        this.eventRepository = eventRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public Reservation createReservation(ReservationRequest req) {
        Event e = eventRepository.findById(req.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));
        Resource r = resourceRepository.findById(req.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        User u = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Reservation res = new Reservation();
        mapRequestToEntity(req, res);
        res.setEvent(e);
        res.setResource(r);
        res.setUser(u);
        return save(res);
    }

    public Reservation updateReservation(Long id, ReservationRequest req) {
        Reservation res = findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        Event e = eventRepository.findById(req.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));
        Resource r = resourceRepository.findById(req.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        User u = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        mapRequestToEntity(req, res);
        res.setEvent(e);
        res.setResource(r);
        res.setUser(u);
        return save(res);
    }

    private void mapRequestToEntity(ReservationRequest req, Reservation res) {
        res.setStartDate(req.getStartDate());
        res.setEndDate(req.getEndDate());
        res.setStatus(req.getStatus());
        res.setQuantityReserved(req.getQuantityReserved());
        res.setNotes(req.getNotes());
    }
}