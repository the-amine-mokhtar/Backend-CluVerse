package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.MaintenanceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for creating vehicle maintenance records.
 * Separates the incoming JSON structure from the JPA entity mapping.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateVehicleMaintenanceDTO {
    private Long vehicleId;             // The vehicle ID from frontend
    private LocalDateTime recordDate;
    
    private int fuelLevel;              // 0-100
    private int engineCondition;        // 0-100
    private int tireCondition;          // 0-100
    private int brakeCondition;         // 0-100
    private int oilLevel;               // 0-100
    
    private int mileage;
    private int kmSinceLastService;
    private int daysSinceLastService;
    private int totalTransports;
    
    private MaintenanceStatus status;
    private String notes;
    private boolean resolved;
}
