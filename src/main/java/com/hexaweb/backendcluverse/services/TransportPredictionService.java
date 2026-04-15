package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.TransportPredictionResponse;
import com.hexaweb.backendcluverse.utils.TunisianCities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Service
public class TransportPredictionService {

    @Autowired
    private LinearRegressionModel model;

    public TransportPredictionResponse predictTransport(
            Long departureLocationId,
            Long arrivalLocationId,
            LocalDateTime scheduledDate,
            String departureName,
            String arrivalName) {

        int hour = scheduledDate.getHour();
        DayOfWeek dow = scheduledDate.getDayOfWeek();
        
        // Extract features for ML model
        boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
        boolean isPeakMorning = (hour >= 7 && hour <= 9);
        boolean isPeakEvening = (hour >= 16 && hour <= 19);
        boolean isLunch = (hour >= 12 && hour <= 14);
        boolean isFriday = (dow == DayOfWeek.FRIDAY);
        boolean isFridayLunch = (isFriday && isLunch);

        // Features array must match train_model.py order:
        // [dep_hour, is_weekend, is_peak_morning, 
        //  is_peak_evening, is_lunch, is_friday_lunch]
        double[] features = {
            (double) hour,
            isWeekend ? 1.0 : 0.0,
            isPeakMorning ? 1.0 : 0.0,
            isPeakEvening ? 1.0 : 0.0,
            isLunch ? 1.0 : 0.0,
            isFridayLunch ? 1.0 : 0.0
        };

        // Get base prediction from ML model
        double mlPrediction = model.predict(features);
        
        // Calculate real distance if city names provided
        double distanceKm = 50.0; // default 50km
        double baseDuration;

        if (departureName != null && arrivalName != null 
            && !departureName.isEmpty() && !arrivalName.isEmpty()) {
            
            double[] depCoords = TunisianCities.findCoordinates(departureName);
            double[] arrCoords = TunisianCities.findCoordinates(arrivalName);
            
            System.out.println("🔍 Distance lookup:");
            System.out.println("  DEP: " + departureName + " → " + java.util.Arrays.toString(depCoords));
            System.out.println("  ARR: " + arrivalName + " → " + java.util.Arrays.toString(arrCoords));
            
            if (depCoords != null && arrCoords != null) {
                // Calculate distance between cities
                double straightKm = TunisianCities.distanceKm(
                    depCoords[0], depCoords[1], 
                    arrCoords[0], arrCoords[1]);
                double roadKm = TunisianCities.roadDistanceKm(straightKm);
                distanceKm = roadKm;
                baseDuration = TunisianCities.estimateDurationMinutes(roadKm);
                
                System.out.println("  Straight: " + String.format("%.1f", straightKm) + "km");
                System.out.println("  Road: " + String.format("%.1f", roadKm) + "km");
            } else {
                // city not found in map, use ML model base
                System.out.println("  ⚠️ Cities not found, using ML model base");
                baseDuration = mlPrediction;
            }
        } else {
            // No city names provided, use ML model base
            System.out.println("🔍 No city names provided, using ML model base");
            baseDuration = mlPrediction;
        }

        // Apply ML traffic multiplier on top of distance-based duration
        // ML model predicts relative traffic factor
        double mlAverage = 75.96; // bias from trained model
        double trafficMultiplier = mlPrediction / mlAverage;
        if (trafficMultiplier < 0.5) trafficMultiplier = 0.5; // floor at 50%
        if (trafficMultiplier > 1.8) trafficMultiplier = 1.8; // cap at 180%

        // Final duration = distance base * traffic factor
        double predictedDuration = baseDuration * trafficMultiplier;

        // Night correction: 22h-6h traffic is always lighter
        if (hour >= 22 || hour <= 6) {
            predictedDuration *= 0.75;
        }
        
        // Weekend night correction
        if (isWeekend && (hour >= 22 || hour <= 6)) {
            predictedDuration *= 0.85;
        }

        // Clamp between 15-600 minutes
        predictedDuration = Math.max(15, Math.min(600, predictedDuration));

        System.out.println("📊 Prediction calculation:");
        System.out.println("  Base duration: " + String.format("%.1f", baseDuration) + "min");
        System.out.println("  Traffic multiplier: " + String.format("%.2f", trafficMultiplier));
        System.out.println("  Before corrections: " + String.format("%.1f", baseDuration * trafficMultiplier) + "min");
        System.out.println("  Final duration: " + String.format("%.0f", predictedDuration) + "min");
        System.out.println("  Distance: " + String.format("%.1f", distanceKm) + "km");

        // Convert duration to risk score (0-100)
        // Training data: min~15min, max~300min
        int risk = (int) Math.min(95, Math.max(5,
            (predictedDuration - 15) * 100.0 / 285.0));

        // Determine risk level
        String riskLevel;
        if (risk < 30) {
            riskLevel = "LOW";
        } else if (risk < 60) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "HIGH";
        }

        // Confidence based on model loaded status and distance calculation
        String confidence = (departureName != null && arrivalName != null) 
            ? "high" : "medium";
        
        // Time slot for reference
        String timeSlot;
        if (hour >= 6 && hour < 12) {
            timeSlot = "MORNING";
        } else if (hour >= 12 && hour < 18) {
            timeSlot = "AFTERNOON";
        } else {
            timeSlot = "EVENING";
        }

        // Build recommendation
        String recommendation = buildRecommendation(
            riskLevel, hour, predictedDuration, distanceKm);

        // Build response
        TransportPredictionResponse response = 
            new TransportPredictionResponse();
        
        response.setDepartureLocationId(departureLocationId);
        response.setArrivalLocationId(arrivalLocationId);
        response.setScheduledDate(scheduledDate);
        response.setPredictedDurationMinutes(
            (int) Math.round(predictedDuration));
        response.setCongestionRisk(risk);
        response.setRiskLevel(riskLevel);
        response.setConfidence(confidence);
        response.setRecommendation(recommendation);
        response.setHistoricalTripsAnalyzed(
            (long) model.getTrainingSamples());
        response.setDayOfWeek(dow.name());
        response.setTimeSlot(timeSlot);
        response.setModelType(
            model.isLoaded() ? "LINEAR_REGRESSION" : "RULE_BASED");
        response.setTrainingDataSize(model.getTrainingSamples());
        response.setDistanceKm(Math.round(distanceKm * 10.0) / 10.0);
        response.setDepartureCity(departureName != null ? departureName : "");
        response.setArrivalCity(arrivalName != null ? arrivalName : "");
        
        return response;
    }

    // Backward compatibility - old method without city names
    public TransportPredictionResponse predictTransport(
            Long departureLocationId,
            Long arrivalLocationId,
            LocalDateTime scheduledDate) {
        return predictTransport(departureLocationId, arrivalLocationId, 
            scheduledDate, null, null);
    }

    private int getEstimatedDistance(Long depId, Long arrId) {
        // Returns estimated km between location pairs
        // Based on typical Tunisian city distances
        if (depId == null || arrId == null) return 50;
        if (depId.equals(arrId)) return 5; // same location = very short
        
        long min = Math.min(depId, arrId);
        long max = Math.max(depId, arrId);
        long diff = max - min;
        
        // Use ID difference as a proxy for distance
        if (diff <= 1) return 20;    // close locations
        if (diff <= 3) return 60;    // medium distance
        if (diff <= 5) return 100;   // far
        if (diff <= 10) return 150;  // very far
        return 200;                  // extremely far
    }

    private String buildRecommendation(
            String riskLevel, int hour, double duration, double distanceKm) {
        
        int durationInt = (int) Math.round(duration);
        String distanceInfo = distanceKm > 0 
            ? " (" + String.format("%.0f", distanceKm) + "km)" 
            : "";
        
        switch (riskLevel) {
            case "LOW":
                return "✅ Créneau idéal" + distanceInfo + ". " +
                       "Trafic fluide attendu. Durée: " + durationInt + " min.";
            
            case "MEDIUM":
                int buffer = (int) Math.round(duration * 0.15);
                return "⚠️ Trafic modéré" + distanceInfo + ". " +
                       "Durée: " + durationInt + " min. Prévoyez " + 
                       buffer + " min de marge.";
            
            case "HIGH":
                int betterHour = (hour >= 16) ? hour + 2 : hour - 2;
                if (betterHour < 0) betterHour = 0;
                if (betterHour > 23) betterHour = 23;
                return "🔴 Congestion probable" + distanceInfo + ". " +
                       "Durée estimée: " + durationInt + " min. " +
                       "Départ recommandé vers " + betterHour + "h.";
            
            default:
                return "Prédiction disponible" + distanceInfo + ".";
        }
    }

    private String buildRecommendation(
            String riskLevel, int hour, double duration) {
        return buildRecommendation(riskLevel, hour, duration, 0);
    }
}

