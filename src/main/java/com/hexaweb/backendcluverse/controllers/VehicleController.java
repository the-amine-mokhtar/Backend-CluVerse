package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.services.VehicleService;
import com.hexaweb.backendcluverse.utils.FuelStations;
import com.hexaweb.backendcluverse.utils.FuelConsumption;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public List<Vehicle> getAll() {
        return vehicleService.findAll();
    }

    @GetMapping("/{id}")
    public Vehicle getById(@PathVariable Long id) {
        return vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Vehicle create(@RequestBody Vehicle vehicle) {
        return vehicleService.save(vehicle);
    }

    @PutMapping("/{id}")
    public Vehicle update(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        vehicle.setId(id);
        return vehicleService.save(vehicle);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        vehicleService.deleteById(id);
    }

    @PostMapping("/recalculate-kilometers")
    public Map<String, Object> recalculateKilometers() {
        long updatedCount = vehicleService.recalculateAllVehicleKilometers();
        return Map.of(
                "success", true,
                "updatedCount", updatedCount,
                "message", "Kilométrages recalculés avec succès"
        );
    }

    @GetMapping("/{id}/fuel-status")
    public Map<String, Object> getFuelStatus(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        Double fuelLevel = vehicle.getFuelLevel() != null ? vehicle.getFuelLevel() : 100.0;
        String status = fuelLevel > 75 ? "GREEN" : (fuelLevel > 50 ? "YELLOW" : (fuelLevel > 25 ? "ORANGE" : "RED"));
        boolean needsFuel = fuelLevel < 25;
        
        // Calculate remaining range
        double remainingRange = FuelConsumption.calculateRemainingRange(fuelLevel, vehicle.getModel());
        
        String message;
        if (fuelLevel > 75) {
            message = "✅ Réservoir plein";
        } else if (fuelLevel > 50) {
            message = "⚠️ Réservoir à " + String.format("%.0f", fuelLevel) + "%";
        } else if (fuelLevel > 25) {
            message = "🟠 Ravitaillement recommandé. Autonomie: " + String.format("%.0f", remainingRange) + " km";
        } else {
            message = "🚨 ALERTE CARBURANT! Ravitaillement URGENT! Autonomie: " + String.format("%.0f", remainingRange) + " km";
        }
        
        return Map.of(
                "vehicleId", id,
                "vehicleModel", vehicle.getModel(),
                "fuelLevel", Math.round(fuelLevel * 10.0) / 10.0,
                "fuelTankCapacity", vehicle.getFuelTankCapacity(),
                "remainingRange", Math.round(remainingRange * 10.0) / 10.0,
                "status", status,
                "needsFuel", needsFuel,
                "message", message
        );
    }

    @GetMapping("/{id}/fuel-stations")
    public Map<String, Object> getNearestFuelStations(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "36.8") double latitude,
                                                      @RequestParam(defaultValue = "10.2") double longitude,
                                                      @RequestParam(defaultValue = "3") int limit) {
        Vehicle vehicle = vehicleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        List<FuelStations.Station> stations = FuelStations.findNearestStations(latitude, longitude, limit);
        List<Map<String, Object>> stationsList = new ArrayList<>();
        
        for (FuelStations.Station station : stations) {
            Map<String, Object> stationMap = new HashMap<>();
            stationMap.put("name", station.name);
            stationMap.put("city", station.city);
            stationMap.put("services", station.services);
            stationMap.put("latitude", station.latitude);
            stationMap.put("longitude", station.longitude);
            stationsList.add(stationMap);
        }
        
        return Map.of(
                "vehicleId", id,
                "vehicleModel", vehicle.getModel(),
                "currentLocation", Map.of("latitude", latitude, "longitude", longitude),
                "nearestStations", stationsList,
                "message", "Stations-services proches"
        );
    }
}
