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

import java.util.List;

@Service
public class ReservationService extends EntityServiceImpl<Reservation, Long> {

    private final EventRepository eventRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository repository,
                              EventRepository eventRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository) {
        super(repository);
        this.eventRepository = eventRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.reservationRepository = repository;
    }

    public Reservation createReservation(ReservationRequest req) {
        Event e = eventRepository.findById(req.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));
        Resource r = resourceRepository.findById(req.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        User u = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (r.getAvailableQuantity() < req.getQuantityReserved()) {
            throw new RuntimeException("Insufficient quantity available. Available: " + r.getAvailableQuantity());
        }

        Reservation res = new Reservation();
        mapRequestToEntity(req, res);
        res.setEvent(e);
        res.setResource(r);
        res.setUser(u);

        // Update available quantity
        r.setAvailableQuantity(r.getAvailableQuantity() - req.getQuantityReserved());
        resourceRepository.save(r);

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

        int oldQuantity = res.getQuantityReserved();
        int newQuantity = req.getQuantityReserved();
        int difference = newQuantity - oldQuantity;

        if (r.getAvailableQuantity() < difference) {
            throw new RuntimeException("Insufficient quantity available. Available: " + r.getAvailableQuantity());
        }

        mapRequestToEntity(req, res);
        res.setEvent(e);
        res.setResource(r);
        res.setUser(u);

        // Update available quantity
        r.setAvailableQuantity(r.getAvailableQuantity() - difference);
        resourceRepository.save(r);

        return save(res);
    }

    private void mapRequestToEntity(ReservationRequest req, Reservation res) {
        res.setStartDate(req.getStartDate());
        res.setEndDate(req.getEndDate());
        res.setStatus(req.getStatus());
        res.setQuantityReserved(req.getQuantityReserved());
        res.setNotes(req.getNotes());
    }

    public List<Reservation> findByEventId(Long eventId) {
        return reservationRepository.findByEventId(eventId);
    }

    @Override
    public void deleteById(Long id) {
        Reservation reservation = findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        Resource resource = reservation.getResource();
        if (resource != null) {
            resource.setAvailableQuantity(resource.getAvailableQuantity() + reservation.getQuantityReserved());
            resourceRepository.save(resource);
        }

        reservationRepository.deleteById(id);
    }
}