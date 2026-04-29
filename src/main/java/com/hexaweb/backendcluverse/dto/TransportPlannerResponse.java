package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransportPlannerResponse {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private int totalReservations;
    private int availableVehicles;
    private List<TransportSuggestion> suggestions;
    private String plannerNote;
}
