package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CVAnalysisResponse {
    private List<CompetencyImpact> suggested_competencies;
    private String overall_description;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompetencyImpact {
        private String name;
        private int level;
        private String category;
        private String reason;
    }
}
