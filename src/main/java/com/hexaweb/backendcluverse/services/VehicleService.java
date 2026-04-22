package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.logistics.Transport;
import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.repositories.TransportRepository;
import com.hexaweb.backendcluverse.repositories.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VehicleService extends EntityServiceImpl<Vehicle, Long> {
    
    private final VehicleRepository vehicleRepository;
    private final TransportRepository transportRepository;
    
    public VehicleService(VehicleRepository repository, TransportRepository transportRepository) {
        super(repository);
        this.vehicleRepository = repository;
        this.transportRepository = transportRepository;
    }

    /**
     * Validate that plate number is unique for the given model.
     * Utilisé pour: "La matricule doit être unique PAR TYPE de véhicule"
     * Exemple: Deux CLIO ne peuvent pas avoir la même matricule
     *          Mais un BUS et une CLIO peuvent avoir la même matricule
     */
    public void validateUniquePlateForModel(Vehicle vehicle) {
        if (vehicle.getPlateNumber() == null || vehicle.getModel() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "❌ Matricule et modèle requis"
            );
        }

        // Use optimized query instead of findAll()
        var existing = vehicleRepository.findByModelAndPlateNumber(
            vehicle.getModel(), 
            vehicle.getPlateNumber()
        );

        if (existing.isPresent()) {
            // Skip if it's the same vehicle (for updates)
            if (vehicle.getId() != null && vehicle.getId().equals(existing.get().getId())) {
                return;
            }

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "❌ Une véhicule avec le modèle '" + vehicle.getModel() + 
                "' et la matricule '" + vehicle.getPlateNumber() + "' existe déjà. " +
                "Les matricules doivent être uniques par TYPE de véhicule."
            );
        }
    }

    /**
     * Override save to validate unique constraint
     */
    @Override
    public Vehicle save(Vehicle vehicle) {
        validateUniquePlateForModel(vehicle);
        return super.save(vehicle);
    }

    /**
     * Recalculate and fix total kilometers for all vehicles.
     * Summa all transport distances for each vehicle.
     * Utilisé pour corriger les véhicules existants avec totalKilometers = NULL
     */
    public long recalculateAllVehicleKilometers() {
        long count = 0;
        
        for (Vehicle vehicle : vehicleRepository.findAll()) {
            double totalKm = 0.0;
            
            // Sum all completed and in-progress transport distances for this vehicle
            for (Transport transport : transportRepository.findAll()) {
                if (transport.getVehicle() != null && 
                    transport.getVehicle().getId().equals(vehicle.getId()) &&
                    transport.getDistance() != null) {
                    totalKm += transport.getDistance();
                }
            }
            
            // Only update if the value changed
            Double currentTotal = vehicle.getTotalKilometers() != null ? vehicle.getTotalKilometers() : 0.0;
            if (Math.abs(currentTotal - totalKm) > 0.01) {
                vehicle.setTotalKilometers(totalKm);
                vehicleRepository.save(vehicle);
                System.out.println("🔧 [Recalculate] Véhicule ID " + vehicle.getId() + 
                                 " (" + vehicle.getModel() + " " + vehicle.getPlateNumber() + 
                                 ") → Total: " + String.format("%.1f", totalKm) + " km");
                count++;
            }
        }
        
        System.out.println("✅ [Recalculate] " + count + " véhicules mis à jour");
        return count;
    }
}

