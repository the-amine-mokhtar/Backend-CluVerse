package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.CreateVehicleMaintenanceDTO;
import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.entities.logistics.VehicleMaintenance;
import com.hexaweb.backendcluverse.enumerations.MaintenanceStatus;
import com.hexaweb.backendcluverse.repositories.VehicleMaintenanceRepository;
import com.hexaweb.backendcluverse.repositories.VehicleRepository;
import com.hexaweb.backendcluverse.services.MaintenancePredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/vehicle-maintenance")
@RequiredArgsConstructor
public class VehicleMaintenanceController {

    private final VehicleMaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenancePredictionService maintenancePredictionService;

    @GetMapping
    public List<VehicleMaintenance> getAll() {
        return maintenanceRepository.findAll();
    }

    @GetMapping("/{id}")
    public VehicleMaintenance getById(@PathVariable Long id) {
        return maintenanceRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public List<VehicleMaintenance> getByVehicleId(@PathVariable Long vehicleId) {
        return maintenanceRepository.findByVehicleIdOrderByRecordDateDesc(vehicleId);
    }

    @GetMapping("/alerts")
    public List<VehicleMaintenance> getAlerts() {
        List<VehicleMaintenance> warnings = 
            maintenanceRepository.findByStatusOrderByRecordDateDesc(MaintenanceStatus.WARNING);
        List<VehicleMaintenance> criticals = 
            maintenanceRepository.findByStatusOrderByRecordDateDesc(MaintenanceStatus.CRITICAL);
        
        warnings.addAll(criticals);
        return warnings.stream().filter(m -> !m.isResolved()).toList();
    }

    @PostMapping
    public VehicleMaintenance create(@RequestBody CreateVehicleMaintenanceDTO dto) {
        // Validate that vehicleId is provided
        if (dto.getVehicleId() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "vehicleId is required"
            );
        }
        
        // Log incoming DTO values
        System.out.println("[VehicleMaintenanceController] Received DTO: vehicleId=" + dto.getVehicleId() + 
                         ", dtoFuelLevel=" + dto.getFuelLevel() + ", dtoMileage=" + dto.getMileage());
        
        // Load the Vehicle from the database
        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, 
                "Vehicle not found with id: " + dto.getVehicleId()
            ));
        
        // Log vehicle data retrieved from DB
        System.out.println("[VehicleMaintenanceController] Vehicle from DB: vehicleId=" + vehicle.getId() + 
                         ", vehicleFuelLevel=" + vehicle.getFuelLevel() + ", vehicleTotalKm=" + vehicle.getTotalKilometers());
        
        // Create the VehicleMaintenance entity and map all fields from DTO
        VehicleMaintenance maintenance = new VehicleMaintenance();
        maintenance.setVehicle(vehicle);  // Set the Vehicle object - this will populate vehicle_id in DB
        maintenance.setRecordDate(dto.getRecordDate() != null ? dto.getRecordDate() : LocalDateTime.now());
        // SYNC: Always use vehicle's current fuelLevel for accuracy
        int syncedFuelLevel = vehicle.getFuelLevel() != null ? vehicle.getFuelLevel().intValue() : 0;
        maintenance.setFuelLevel(syncedFuelLevel);
        maintenance.setEngineCondition(dto.getEngineCondition());
        maintenance.setTireCondition(dto.getTireCondition());
        maintenance.setBrakeCondition(dto.getBrakeCondition());
        maintenance.setOilLevel(dto.getOilLevel());
        // SYNC: Always use vehicle's totalKilometers as mileage for accuracy
        int syncedMileage = vehicle.getTotalKilometers() != null ? vehicle.getTotalKilometers().intValue() : 0;
        maintenance.setMileage(syncedMileage);
        maintenance.setKmSinceLastService(dto.getKmSinceLastService());
        maintenance.setDaysSinceLastService(dto.getDaysSinceLastService());
        maintenance.setTotalTransports(dto.getTotalTransports());
        maintenance.setNotes(dto.getNotes());
        maintenance.setResolved(dto.isResolved());
        
        // AUTOMATIC: Calculate status based on ML risk prediction
        double risk = maintenancePredictionService.calculateRisk(
            dto.getKmSinceLastService(),
            dto.getDaysSinceLastService(),
            syncedFuelLevel,
            dto.getEngineCondition(),
            dto.getTireCondition(),
            dto.getBrakeCondition(),
            dto.getOilLevel(),
            dto.getTotalTransports()
        );
        
        MaintenanceStatus calculatedStatus;
        if (risk >= 65) {
            calculatedStatus = MaintenanceStatus.CRITICAL;
        } else if (risk >= 30) {
            calculatedStatus = MaintenanceStatus.WARNING;
        } else {
            calculatedStatus = MaintenanceStatus.GOOD;
        }
        
        // Use calculated status if DTO status is not explicitly provided
        MaintenanceStatus finalStatus = dto.getStatus() != null ? dto.getStatus() : calculatedStatus;
        maintenance.setStatus(finalStatus);
        
        // AUTO-RESOLVE: If the new maintenance record shows the vehicle is GOOD,
        // automatically resolve all previous WARNING/CRITICAL alerts for this vehicle
        if (finalStatus == MaintenanceStatus.GOOD) {
            List<VehicleMaintenance> previousAlerts = maintenanceRepository.findByVehicleIdOrderByRecordDateDesc(vehicle.getId())
                .stream()
                .filter(m -> !m.isResolved() && (m.getStatus() == MaintenanceStatus.WARNING || m.getStatus() == MaintenanceStatus.CRITICAL))
                .toList();
            
            for (VehicleMaintenance alert : previousAlerts) {
                alert.setResolved(true);
                maintenanceRepository.save(alert);
                System.out.println("[VehicleMaintenanceController] ✓ Auto-resolved previous alert ID " + alert.getId() + " for vehicle " + vehicle.getId());
            }
        }
        
        // Log the synchronized values for debugging
        System.out.println("[VehicleMaintenanceController] ✓ SAVED maintenance record for vehicle " + vehicle.getId() + 
                         ": FINAL fuelLevel=" + syncedFuelLevel + "%, FINAL mileage=" + syncedMileage + " km, STATUS=" + finalStatus);
        
        return maintenanceRepository.save(maintenance);
    }

    @PutMapping("/{id}")
    public VehicleMaintenance update(@PathVariable Long id, @RequestBody VehicleMaintenance maintenance) {
        VehicleMaintenance existing = maintenanceRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        existing.setFuelLevel(maintenance.getFuelLevel());
        existing.setEngineCondition(maintenance.getEngineCondition());
        existing.setTireCondition(maintenance.getTireCondition());
        existing.setBrakeCondition(maintenance.getBrakeCondition());
        existing.setOilLevel(maintenance.getOilLevel());
        existing.setMileage(maintenance.getMileage());
        existing.setKmSinceLastService(maintenance.getKmSinceLastService());
        existing.setDaysSinceLastService(maintenance.getDaysSinceLastService());
        existing.setTotalTransports(maintenance.getTotalTransports());
        existing.setNotes(maintenance.getNotes());
        
        // AUTOMATIC: Recalculate status on update based on ML risk prediction
        double risk = maintenancePredictionService.calculateRisk(
            maintenance.getKmSinceLastService(),
            maintenance.getDaysSinceLastService(),
            maintenance.getFuelLevel(),
            maintenance.getEngineCondition(),
            maintenance.getTireCondition(),
            maintenance.getBrakeCondition(),
            maintenance.getOilLevel(),
            maintenance.getTotalTransports()
        );
        
        MaintenanceStatus calculatedStatus;
        if (risk >= 65) {
            calculatedStatus = MaintenanceStatus.CRITICAL;
        } else if (risk >= 30) {
            calculatedStatus = MaintenanceStatus.WARNING;
        } else {
            calculatedStatus = MaintenanceStatus.GOOD;
        }
        
        existing.setStatus(maintenance.getStatus() != null && !maintenance.getStatus().equals(MaintenanceStatus.GOOD) 
            ? maintenance.getStatus() : calculatedStatus);
        
        // AUTO-RESOLVE: If the updated maintenance record shows the vehicle is GOOD,
        // automatically resolve all previous WARNING/CRITICAL alerts for this vehicle
        if (existing.getStatus() == MaintenanceStatus.GOOD) {
            List<VehicleMaintenance> previousAlerts = maintenanceRepository.findByVehicleIdOrderByRecordDateDesc(existing.getVehicle().getId())
                .stream()
                .filter(m -> !m.isResolved() && m.getId() != existing.getId() && 
                        (m.getStatus() == MaintenanceStatus.WARNING || m.getStatus() == MaintenanceStatus.CRITICAL))
                .toList();
            
            for (VehicleMaintenance alert : previousAlerts) {
                alert.setResolved(true);
                maintenanceRepository.save(alert);
                System.out.println("[VehicleMaintenanceController] ✓ Auto-resolved previous alert ID " + alert.getId() + " for vehicle " + existing.getVehicle().getId());
            }
        }
        
        return maintenanceRepository.save(existing);
    }

    @PutMapping("/{id}/resolve")
    public VehicleMaintenance resolve(@PathVariable Long id) {
        VehicleMaintenance maintenance = maintenanceRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        maintenance.setResolved(true);
        return maintenanceRepository.save(maintenance);
    }

    /**
     * Synchronize all maintenance records with their vehicle's current fuel level and mileage
     * Corrects records that were created before synchronization was implemented
     */
    @PostMapping("/sync/all")
    public Map<String, Object> synchronizeAllRecords() {
        List<VehicleMaintenance> allRecords = maintenanceRepository.findAll();
        int updated = 0;
        int failed = 0;
        
        System.out.println("[VehicleMaintenanceController] Starting full synchronization of " + allRecords.size() + " maintenance records");
        
        for (VehicleMaintenance record : allRecords) {
            try {
                Vehicle vehicle = record.getVehicle();
                if (vehicle != null) {
                    int newFuelLevel = vehicle.getFuelLevel() != null ? vehicle.getFuelLevel().intValue() : 0;
                    int newMileage = vehicle.getTotalKilometers() != null ? vehicle.getTotalKilometers().intValue() : 0;
                    
                    record.setFuelLevel(newFuelLevel);
                    record.setMileage(newMileage);
                    maintenanceRepository.save(record);
                    
                    System.out.println("[VehicleMaintenanceController] ✓ Synced record " + record.getId() + 
                                     " for vehicle " + vehicle.getId() + ": fuel=" + newFuelLevel + "%, mileage=" + newMileage + " km");
                    updated++;
                }
            } catch (Exception e) {
                System.err.println("[VehicleMaintenanceController] ✗ Failed to sync record " + record.getId() + ": " + e.getMessage());
                failed++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalRecords", allRecords.size());
        result.put("updated", updated);
        result.put("failed", failed);
        result.put("message", "Synchronization complete");
        
        System.out.println("[VehicleMaintenanceController] Synchronization complete: " + updated + " updated, " + failed + " failed");
        
        return result;
    }
}
