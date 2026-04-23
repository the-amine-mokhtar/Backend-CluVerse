package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyStatsCompetencyResponse {
    private Long competencyId;
    private String competencyName;
    private Long memberCount;
    private Double averageCurrentLevel;
    private Double averageTargetLevel;
    private Double averageGap;
}