package com.hexaweb.backendcluverse.dto.Competencies;

import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyStatsCategoryResponse {
    private CompetencyType category;
    private Long competencyCount;
    private Long memberCount;
    private Double averageCurrentLevel;
    private Double averageTargetLevel;
    private Double averageGap;
}