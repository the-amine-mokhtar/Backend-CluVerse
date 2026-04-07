package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.TransportRequest;
import com.hexaweb.backendcluverse.dto.TransportResponse;
import com.hexaweb.backendcluverse.entities.logistics.Transport;
import com.hexaweb.backendcluverse.services.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transports")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    private TransportResponse toResponse(Transport transport) {
        if (transport == null) {
            return null;
        }

        Long vehicleId = transport.getVehicleId();
        if (vehicleId == null && transport.getVehicle() != null) {
            vehicleId = transport.getVehicle().getId();
        }

        Long userId = transport.getUserId();
        if (userId == null && transport.getUser() != null) {
            userId = transport.getUser().getId();
        }

        Long eventId = transport.getEventId();
        if (eventId == null && transport.getEvent() != null) {
            eventId = transport.getEvent().getId();
        }

        return new TransportResponse(
                transport.getId(),
                transport.getScheduledDate(),
                transport.getDepartureLocationId(),
                transport.getArrivalLocationId(),
                transport.getStatus(),
                vehicleId,
                userId,
                eventId
        );
    }

    @GetMapping
    public List<TransportResponse> getAll() {
        return transportService.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public TransportResponse getById(@PathVariable Long id) {
        Transport transport = transportService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toResponse(transport);
    }

    @PostMapping
    public TransportResponse create(@RequestBody TransportRequest request) {
        return toResponse(transportService.createTransport(request));
    }

    @PutMapping("/{id}")
    public TransportResponse update(@PathVariable Long id, @RequestBody TransportRequest request) {
        return toResponse(transportService.updateTransport(id, request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        transportService.deleteById(id);
    }

    @PostMapping("/update-statuses")
    public Map<String, Object> updateStatuses() {
        long updatedCount = transportService.updateTransportStatuses();
        return Map.of(
                "success", true,
                "updatedCount", updatedCount,
                "message", "Statuts transportés mis à jour avec succès"
        );
    }
}
