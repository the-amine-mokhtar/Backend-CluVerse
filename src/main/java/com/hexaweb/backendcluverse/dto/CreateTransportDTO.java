package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.TransportStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for creating/updating transport records.
 * Separates the incoming JSON structure from the JPA entity mapping.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransportDTO {
    private LocalDateTime scheduledDate;
    private Long departureLocationId;
    private Long arrivalLocationId;
    private Double distance;  // Distance in km
    private TransportStatus status;
    private Long vehicleId;   // Will be resolved to Vehicle object
    private Long userId;      // Will be resolved to User object
    private Long eventId;     // Will be resolved to Event object (nullable)
}
