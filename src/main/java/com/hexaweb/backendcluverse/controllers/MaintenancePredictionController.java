package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.MaintenancePredictionResponse;
import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.entities.logistics.VehicleMaintenance;
import com.hexaweb.backendcluverse.repositories.VehicleMaintenanceRepository;
import com.hexaweb.backendcluverse.repositories.VehicleRepository;
import com.hexaweb.backendcluverse.services.MaintenancePredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/maintenance-prediction")
@RequiredArgsConstructor
public class MaintenancePredictionController {

    private final MaintenancePredictionService predictionService;
    private final VehicleMaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;

    @GetMapping("/{vehicleId}")
    public MaintenancePredictionResponse predictFromLatestRecord(@PathVariable Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        
        VehicleMaintenance latest = maintenanceRepository.findTopByVehicleIdOrderByRecordDateDesc(vehicleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "No maintenance records found for this vehicle"));
        
        return predictionService.predict(
            vehicleId,
            vehicle.getModel(),
            vehicle.getPlateNumber(),
            latest.getKmSinceLastService(),
            latest.getDaysSinceLastService(),
            latest.getFuelLevel(),
            latest.getEngineCondition(),
            latest.getTireCondition(),
            latest.getBrakeCondition(),
            latest.getOilLevel(),
            latest.getTotalTransports()
        );
    }

    @PostMapping("/{vehicleId}/analyze")
    public MaintenancePredictionResponse analyzeMaintenanceData(
            @PathVariable Long vehicleId,
            @RequestBody VehicleMaintenance maintenance) {
        
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        
        return predictionService.predict(
            vehicleId,
            vehicle.getModel(),
            vehicle.getPlateNumber(),
            maintenance.getKmSinceLastService(),
            maintenance.getDaysSinceLastService(),
            maintenance.getFuelLevel(),
            maintenance.getEngineCondition(),
            maintenance.getTireCondition(),
            maintenance.getBrakeCondition(),
            maintenance.getOilLevel(),
            maintenance.getTotalTransports()
        );
    }
}
