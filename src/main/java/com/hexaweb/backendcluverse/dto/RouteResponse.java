package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private String tripId;
    private String routeName;
    private List<RouteStep> steps;
    private int totalStops;
    private String totalDuration; // "X min"
}
