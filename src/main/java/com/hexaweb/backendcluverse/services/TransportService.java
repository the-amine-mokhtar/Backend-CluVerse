package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.TransportRequest;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.logistics.Transport;
import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.entities.logistics.VehicleMaintenance;
import com.hexaweb.backendcluverse.enumerations.TransportStatus;
import com.hexaweb.backendcluverse.enumerations.MaintenanceStatus;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import com.hexaweb.backendcluverse.repositories.TransportRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.repositories.VehicleRepository;
import com.hexaweb.backendcluverse.repositories.VehicleMaintenanceRepository;
import com.hexaweb.backendcluverse.utils.FuelConsumption;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransportService extends EntityServiceImpl<Transport, Long> {

    private final TransportRepository transportRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;

    public TransportService(TransportRepository repository, VehicleRepository vehicleRepository, UserRepository userRepository, EventRepository eventRepository, VehicleMaintenanceRepository vehicleMaintenanceRepository) {
        super(repository);
        this.transportRepository = repository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.vehicleMaintenanceRepository = vehicleMaintenanceRepository;
    }

    /**
     * Override findAll to automatically update transport statuses based on scheduled dates
     */
    @Override
    public List<Transport> findAll() {
        // Update statuses for any transports whose dates have passed
        updateTransportStatuses();
        // Return the updated list
        return transportRepository.findAll();
    }

    public Transport createTransport(TransportRequest req) {
        Vehicle v = vehicleRepository.findById(req.getVehicleId())
            .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        User u = userRepository.findById(req.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if vehicle has no fuel
        Double currentFuel = v.getFuelLevel() != null ? v.getFuelLevel() : 100.0;
        if (currentFuel <= 0) {
            throw new RuntimeException("Cannot create transport: Vehicle has no fuel. Please refuel first.");
        }
        
        Transport t = new Transport();
        mapRequestToEntity(req, t);
        t.setVehicle(v);
        t.setUser(u);
        
        // Add distance to vehicle's total kilometers
        if (req.getDistance() != null && req.getDistance() > 0) {
            v.setTotalKilometers((v.getTotalKilometers() != null ? v.getTotalKilometers() : 0.0) + req.getDistance());
            
            // Calculate fuel consumption
            double fuelConsumptionPercent = FuelConsumption.calculateConsumptionPercent(v.getModel(), req.getDistance());
            double fuelLevelBefore = v.getFuelLevel() != null ? v.getFuelLevel() : 100.0;
            double fuelLevelAfter = Math.max(0.0, fuelLevelBefore - fuelConsumptionPercent);
            
            t.setFuelConsumed(fuelConsumptionPercent);
            t.setFuelLevelAfter(fuelLevelAfter);
            
            // Update vehicle fuel level
            v.setFuelLevel(fuelLevelAfter);
            vehicleRepository.save(v);
            
            // SYNC: Update latest maintenance record with new fuel and mileage
            updateMaintenanceAfterTransport(v);
            
            String fuelAlert = fuelLevelAfter < 25 ? " 🚨 ALERTE CARBURANT!" : "";
            System.out.println("✅ [Transport] Kilométrage: +" + String.format("%.1f", req.getDistance()) + " km | " +
                             "Carburant: " + String.format("%.1f", fuelLevelBefore) + "% → " + String.format("%.1f", fuelLevelAfter) + 
                             "% (-" + String.format("%.1f", fuelConsumptionPercent) + "%)" + fuelAlert);
        }
        
        return save(t);
    }

    public Transport updateTransport(Long id, TransportRequest req) {
        Transport t = findById(id)
            .orElseThrow(() -> new RuntimeException("Transport not found"));
        Vehicle oldVehicle = t.getVehicle();
        Double oldDistance = t.getDistance();
        
        Vehicle newVehicle = vehicleRepository.findById(req.getVehicleId())
            .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        User u = userRepository.findById(req.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Double newDistance = req.getDistance();
        
        // Handle vehicle change or distance change
        if (oldVehicle != null && oldDistance != null && oldDistance > 0) {
            // Subtract old distance from old vehicle
            oldVehicle.setTotalKilometers(Math.max(0.0, 
                (oldVehicle.getTotalKilometers() != null ? oldVehicle.getTotalKilometers() : 0.0) - oldDistance));
            vehicleRepository.save(oldVehicle);
            System.out.println("✅ [Transport Update] Kilométrage du véhicule ID " + oldVehicle.getId() + 
                             " diminue de " + String.format("%.1f", oldDistance) + " km → Total: " + 
                             String.format("%.1f", oldVehicle.getTotalKilometers()) + " km");
        }
        
        // Add new distance to new vehicle
        if (newDistance != null && newDistance > 0) {
            newVehicle.setTotalKilometers((newVehicle.getTotalKilometers() != null ? newVehicle.getTotalKilometers() : 0.0) + newDistance);
            vehicleRepository.save(newVehicle);
            System.out.println("✅ [Transport Update] Kilométrage du véhicule ID " + newVehicle.getId() + 
                             " augmente de " + String.format("%.1f", newDistance) + " km → Total: " + 
                             String.format("%.1f", newVehicle.getTotalKilometers()) + " km");
        }
        
        mapRequestToEntity(req, t);
        t.setVehicle(newVehicle);
        t.setUser(u);
        return save(t);
    }

    private void mapRequestToEntity(TransportRequest req, Transport t) {
        t.setScheduledDate(req.getScheduledDate());
        t.setDepartureLocationId(req.getDepartureLocationId());
        t.setArrivalLocationId(req.getArrivalLocationId());
        t.setDistance(req.getDistance());  // Add distance mapping!
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
    @Override
    public void deleteById(Long id) {
        var transport = findById(id);
        if (transport.isPresent()) {
            Transport t = transport.get();
            if (t.getVehicle() != null && t.getDistance() != null && t.getDistance() > 0) {
                Vehicle v = t.getVehicle();
                v.setTotalKilometers(Math.max(0.0, 
                    (v.getTotalKilometers() != null ? v.getTotalKilometers() : 0.0) - t.getDistance()));
                vehicleRepository.save(v);
                System.out.println("✅ [Transport Delete] Kilométrage du véhicule ID " + v.getId() + 
                                 " diminue de " + String.format("%.1f", t.getDistance()) + " km → Total: " + 
                                 String.format("%.1f", v.getTotalKilometers()) + " km");
            }
        }
        super.deleteById(id);
    }

    /**
     * Auto-update transport statuses based on scheduled date:
     * - PLANNED with past date → COMPLETED
     * - PLANNED with current/future date → IN_PROGRESS (if date reached)
     */
    @Transactional
    @Scheduled(fixedRate = 300000) // Run every 5 minutes (300000 ms)
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

        if (count > 0) {
            System.out.println("✅ [Scheduled Task] Updated " + count + " transport statuses at " + now);
        }

        return count;
    }

    /**
     * Synchronize the latest maintenance record with the vehicle's current fuel and mileage
     * after a transport operation updates the vehicle. Creates initial record if none exists.
     */
    private void updateMaintenanceAfterTransport(Vehicle vehicle) {
        try {
            java.util.List<VehicleMaintenance> records = vehicleMaintenanceRepository.findByVehicleIdOrderByRecordDateDesc(vehicle.getId());
            
            int newFuelLevel = vehicle.getFuelLevel() != null ? vehicle.getFuelLevel().intValue() : 0;
            int newMileage = vehicle.getTotalKilometers() != null ? vehicle.getTotalKilometers().intValue() : 0;
            
            if (!records.isEmpty()) {
                // Update existing maintenance record
                VehicleMaintenance latest = records.get(0);
                latest.setFuelLevel(newFuelLevel);
                latest.setMileage(newMileage);
                vehicleMaintenanceRepository.save(latest);
                
                System.out.println("✅ [TransportService] Synchronized maintenance record #" + latest.getId() + 
                                 " for vehicle #" + vehicle.getId() + 
                                 " | Fuel: " + newFuelLevel + "%, Mileage: " + newMileage + "km");
            } else {
                // Create initial maintenance record if none exists
                VehicleMaintenance initialMaintenance = new VehicleMaintenance();
                initialMaintenance.setVehicle(vehicle);
                initialMaintenance.setRecordDate(LocalDateTime.now());
                initialMaintenance.setFuelLevel(newFuelLevel);
                initialMaintenance.setMileage(newMileage);
                initialMaintenance.setEngineCondition(85);
                initialMaintenance.setTireCondition(85);
                initialMaintenance.setBrakeCondition(85);
                initialMaintenance.setOilLevel(85);
                initialMaintenance.setStatus(MaintenanceStatus.GOOD);
                initialMaintenance.setResolved(false);
                
                VehicleMaintenance saved = vehicleMaintenanceRepository.save(initialMaintenance);
                System.out.println("✅ [TransportService] Created initial maintenance record #" + saved.getId() + 
                                 " for vehicle #" + vehicle.getId() + 
                                 " | Fuel: " + newFuelLevel + "%, Mileage: " + newMileage + "km");
            }
        } catch (Exception e) {
            System.err.println("❌ [TransportService] Error synchronizing maintenance record: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
