package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.TransportRequest;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.logistics.Transport;
import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.repositories.TransportRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.repositories.VehicleRepository;
import org.springframework.stereotype.Service;

@Service
public class TransportService extends EntityServiceImpl<Transport, Long> {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public TransportService(TransportRepository repository, VehicleRepository vehicleRepository, UserRepository userRepository) {
        super(repository);
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
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
    }
}
