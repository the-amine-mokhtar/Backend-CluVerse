package com.hexaweb.backendcluverse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransportSuggestion {
    private int rank;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime suggestedDate;
    
    private Long vehicleId;
    private String vehicleModel;
    private String vehiclePlate;
    private Long resourceId;
    private String resourceName;
    private int totalQuantity;
    private int reservationCount;
    private String reason;
    private String predictedDuration;
    private int congestionRisk;
    private String riskLevel;
}
