package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyStatsResponse {
    private Long clubId;
    private Long totalCompetencies;
    private Long totalAssignments;
    private List<CompetencyStatsCompetencyResponse> membersPerCompetency;
    private List<CompetencyStatsCategoryResponse> averageLevelByCategory;
    private List<CompetencyStatsWeakCompetencyResponse> weakestCompetencies;
}