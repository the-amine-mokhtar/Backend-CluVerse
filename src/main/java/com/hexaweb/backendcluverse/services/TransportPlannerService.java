package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.TransportPlannerResponse;
import com.hexaweb.backendcluverse.dto.TransportPredictionResponse;
import com.hexaweb.backendcluverse.dto.TransportSuggestion;
import com.hexaweb.backendcluverse.entities.event.Reservation;
import com.hexaweb.backendcluverse.entities.logistics.Resource;
import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.enumerations.ReservationStatus;
import com.hexaweb.backendcluverse.repositories.ReservationRepository;
import com.hexaweb.backendcluverse.repositories.ResourceRepository;
import com.hexaweb.backendcluverse.repositories.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransportPlannerService {

    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired  
    private VehicleRepository vehicleRepository;
    
    @Autowired
    private ResourceRepository resourceRepository;
    
    @Autowired
    private TransportPredictionService predictionService;

    public TransportPlannerResponse generatePlan(
            LocalDate startDate, LocalDate endDate) {
        
        // 1. Get reservations in date range
        List<Reservation> reservations = reservationRepository
            .findByStartDateBetween(
                startDate.atStartOfDay(), 
                endDate.atTime(23, 59, 59));
        
        // Filter only PENDING and CONFIRMED
        List<Reservation> active = reservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.PENDING
                      || r.getStatus() == ReservationStatus.CONFIRMED)
            .collect(Collectors.toList());

        // 2. Get available vehicles
        List<Vehicle> vehicles = vehicleRepository
            .findByIsAvailableTrue();

        // 3. Group reservations by resourceId
        Map<Long, List<Reservation>> byResource = active.stream()
            .collect(Collectors.groupingBy(
                r -> r.getResource().getId()));

        // 4. Generate suggestions (max 5)
        List<TransportSuggestion> suggestions = new ArrayList<>();
        int rank = 1;
        
        // Sort by reservation count descending (most needed first)
        List<Map.Entry<Long, List<Reservation>>> sorted = 
            byResource.entrySet().stream()
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .limit(5)
                .collect(Collectors.toList());

        for (Map.Entry<Long, List<Reservation>> entry : sorted) {
            if (vehicles.isEmpty()) break;
            
            Long resourceId = entry.getKey();
            List<Reservation> resList = entry.getValue();
            
            // Pick earliest reservation date as suggested transport date
            LocalDateTime suggestedDate = resList.stream()
                .map(Reservation::getStartDate)
                .min(LocalDateTime::compareTo)
                .orElse(startDate.atTime(9, 0));
            
            // Suggest best time: if peak hour, move to optimal time
            int hour = suggestedDate.getHour();
            if (hour >= 7 && hour <= 9) {
                suggestedDate = suggestedDate.withHour(6);
            } else if (hour >= 16 && hour <= 19) {
                suggestedDate = suggestedDate.withHour(15);
            }

            // Assign vehicle (round-robin)
            Vehicle vehicle = vehicles.get(
                (rank - 1) % vehicles.size());
            
            // Get resource info
            Resource resource = resourceRepository
                .findById(resourceId).orElse(null);
            String resourceName = resource != null ? 
                resource.getName() : "Ressource #" + resourceId;
            
            // Sum total quantity
            int totalQty = resList.stream()
                .mapToInt(Reservation::getQuantityReserved).sum();

            // Get ML prediction for this time slot
            TransportPredictionResponse prediction = 
                predictionService.predictTransport(1L, 2L, suggestedDate);

            // Build reason text
            String reason = buildReason(resList.size(), 
                totalQty, suggestedDate, prediction.getRiskLevel());

            TransportSuggestion suggestion = new TransportSuggestion();
            suggestion.setRank(rank++);
            suggestion.setSuggestedDate(suggestedDate);
            suggestion.setVehicleId(vehicle.getId());
            suggestion.setVehicleModel(vehicle.getModel());
            suggestion.setVehiclePlate(vehicle.getPlateNumber());
            suggestion.setResourceId(resourceId);
            suggestion.setResourceName(resourceName);
            suggestion.setTotalQuantity(totalQty);
            suggestion.setReservationCount(resList.size());
            suggestion.setReason(reason);
            suggestion.setPredictedDuration(
                prediction.getPredictedDurationMinutes() + " min");
            suggestion.setCongestionRisk(prediction.getCongestionRisk());
            suggestion.setRiskLevel(prediction.getRiskLevel());
            suggestions.add(suggestion);
        }

        // Build response
        TransportPlannerResponse response = new TransportPlannerResponse();
        response.setWeekStart(startDate);
        response.setWeekEnd(endDate);
        response.setTotalReservations(active.size());
        response.setAvailableVehicles(vehicles.size());
        response.setSuggestions(suggestions);
        response.setPlannerNote(
            suggestions.isEmpty() 
            ? "Aucune réservation active sur cette période."
            : "Plan généré par IA basé sur " + active.size() 
              + " réservations et " + vehicles.size() 
              + " véhicules disponibles.");
        
        return response;
    }

    private String buildReason(int count, int qty, 
            LocalDateTime date, String riskLevel) {
        String timeInfo = riskLevel.equals("HIGH") 
            ? "Heure ajustée pour éviter la congestion." 
            : "Créneau optimal.";
        return count + " réservation(s) groupée(s) — " 
            + qty + " unité(s) à transporter. " + timeInfo;
    }
}
