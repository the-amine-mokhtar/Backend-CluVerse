package com.hexaweb.backendcluverse.dto.Competencies;

import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyStatsWeakCompetencyResponse {
    private Long competencyId;
    private String competencyName;
    private CompetencyType category;
    private Long memberCount;
    private Double averageCurrentLevel;
    private Double averageTargetLevel;
    private Double averageGap;
}