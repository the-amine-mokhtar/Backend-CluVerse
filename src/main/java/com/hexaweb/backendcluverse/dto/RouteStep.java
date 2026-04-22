package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteStep {
    private int sequence;
    private String stopId;
    private String stopName;
    private double latitude;
    private double longitude;
    private String arrivalTime;
    private String departureTime;
}
