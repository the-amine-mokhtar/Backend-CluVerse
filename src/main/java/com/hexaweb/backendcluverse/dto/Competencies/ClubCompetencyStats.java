package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubCompetencyStats {
    private Integer totalMembers;
    private Double avgLevelAcrossAll;
    private Integer totalGaps;
    private Integer criticalGaps;
}
