package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.TransportPredictionResponse;
import com.hexaweb.backendcluverse.dto.TransportRequest;
import com.hexaweb.backendcluverse.dto.TransportResponse;
import com.hexaweb.backendcluverse.entities.logistics.Transport;
import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.services.TransportPredictionService;
import com.hexaweb.backendcluverse.services.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transports")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;
    private final TransportPredictionService transportPredictionService;

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
                transport.getDistance(),
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

    @GetMapping("/distance-between")
    public Map<String, Object> getDistanceBetween(
            @RequestParam Long departureLocationId,
            @RequestParam Long arrivalLocationId) {
        Double distance = transportPredictionService.calculateDistanceBetweenLocations(
                departureLocationId, arrivalLocationId);
        return Map.of(
                "departureLocationId", departureLocationId,
                "arrivalLocationId", arrivalLocationId,
                "distanceKm", distance != null ? distance : 0.0
        );
    }

    @GetMapping(value = "/prediction", 
               produces = "application/json;charset=UTF-8")
    public TransportPredictionResponse getPrediction(
            @RequestParam Long departureLocationId,
            @RequestParam Long arrivalLocationId,
            @RequestParam String scheduledDate,
            @RequestParam(required = false) String departureName,
            @RequestParam(required = false) String arrivalName) {
        LocalDateTime dateTime = LocalDateTime.parse(scheduledDate);
        return transportPredictionService.predictTransport(
                departureLocationId, arrivalLocationId, dateTime, 
                departureName, arrivalName);
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

    @GetMapping("/{id}/fuel-alert")
    public Map<String, Object> getFuelAlert(@PathVariable Long id) {
        Transport transport = transportService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        Vehicle vehicle = transport.getVehicle();
        Double fuelLevel = vehicle.getFuelLevel() != null ? vehicle.getFuelLevel() : 100.0;
        
        boolean needsFuel = fuelLevel < 25;
        String status = fuelLevel > 75 ? "GREEN" : (fuelLevel > 50 ? "YELLOW" : (fuelLevel > 25 ? "ORANGE" : "RED"));
        String message = needsFuel ? "🚨 RAVITAILLEMENT RECOMMANDÉ!" : "✅ Niveau de carburant acceptable";
        
        return Map.of(
                "transportId", id,
                "vehicleId", vehicle.getId(),
                "vehicleModel", vehicle.getModel(),
                "fuelLevel", Math.round(fuelLevel * 10.0) / 10.0,
                "fuelConsumed", transport.getFuelConsumed() != null ? Math.round(transport.getFuelConsumed() * 10.0) / 10.0 : 0.0,
                "needsFuel", needsFuel,
                "status", status,
                "message", message
        );
    }
}

