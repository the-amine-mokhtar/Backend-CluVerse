package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CompetencyMatchCandidateResponse {
    private Long userId;
    private String memberName;
    private String memberEmail;
    private Double score;
    private String readiness;
    private Integer matchedSkills;
    private Integer totalRequiredSkills;
    private Double averageGap;
    private List<String> missingSkills;
}
