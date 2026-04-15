package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.TransportRequest;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.logistics.Transport;
import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.enumerations.TransportStatus;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import com.hexaweb.backendcluverse.repositories.TransportRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.repositories.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransportService extends EntityServiceImpl<Transport, Long> {

    private final TransportRepository transportRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public TransportService(TransportRepository repository, VehicleRepository vehicleRepository, UserRepository userRepository, EventRepository eventRepository) {
        super(repository);
        this.transportRepository = repository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    public Transport createTransport(TransportRequest req) {
        Vehicle v = vehicleRepository.findById(req.getVehicleId())
            .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        User u = userRepository.findById(req.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        Transport t = new Transport();
        mapRequestToEntity(req, t);
        t.setVehicle(v);
        t.setUser(u);
        return save(t);
    }

    public Transport updateTransport(Long id, TransportRequest req) {
        Transport t = findById(id)
            .orElseThrow(() -> new RuntimeException("Transport not found"));
        Vehicle v = vehicleRepository.findById(req.getVehicleId())
            .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        User u = userRepository.findById(req.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        mapRequestToEntity(req, t);
        t.setVehicle(v);
        t.setUser(u);
        return save(t);
    }

    private void mapRequestToEntity(TransportRequest req, Transport t) {
        t.setScheduledDate(req.getScheduledDate());
        t.setDepartureLocationId(req.getDepartureLocationId());
        t.setArrivalLocationId(req.getArrivalLocationId());
        t.setStatus(req.getStatus());

        if (req.getEventId() == null) {
            t.setEvent(null);
        } else {
            Event event = eventRepository.findById(req.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));
            t.setEvent(event);
        }
    }

    /**
     * Auto-update transport statuses based on scheduled date:
     * - PLANNED with past date → COMPLETED
     * - PLANNED with current/future date → IN_PROGRESS (if date reached)
     */
    @Transactional
    public long updateTransportStatuses() {
        LocalDateTime now = LocalDateTime.now();
        long count = 0;

        TransportRepository repo = transportRepository;
        
        // Find all PLANNED transports with past dates and mark as COMPLETED
        for (Transport t : repo.findAll()) {
            if (t.getStatus() == TransportStatus.PLANNED && t.getScheduledDate() != null) {
                if (t.getScheduledDate().isBefore(now)) {
                    t.setStatus(TransportStatus.COMPLETED);
                    t.setCompletedAt(LocalDateTime.now());
                    save(t);
                    count++;
                }
                // If date is now or just reached, change to IN_PROGRESS
                else if (t.getScheduledDate().isBefore(now.plusMinutes(5)) && t.getScheduledDate().isAfter(now.minusMinutes(5))) {
                    t.setStatus(TransportStatus.IN_PROGRESS);
                    save(t);
                    count++;
                }
            }
        }

        return count;
    }
}
