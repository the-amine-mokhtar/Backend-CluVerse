package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransportPredictionResponse {
    private Long departureLocationId;
    private Long arrivalLocationId;
    private LocalDateTime scheduledDate;
    private Integer predictedDurationMinutes;
    private Integer congestionRisk; // 0-100
    private String riskLevel; // LOW, MEDIUM, HIGH
    private String confidence; // low, medium, high
    private String recommendation;
    private Long historicalTripsAnalyzed;
    private String dayOfWeek; // MONDAY, TUESDAY, etc.
    private String timeSlot; // MORNING, AFTERNOON, EVENING
    private String modelType; // LINEAR_REGRESSION or RULE_BASED
    private Integer trainingDataSize; // Number of training samples
    private Double distanceKm; // Real distance between cities in km
    private String departureCity; // City name of departure
    private String arrivalCity; // City name of arrival
}
