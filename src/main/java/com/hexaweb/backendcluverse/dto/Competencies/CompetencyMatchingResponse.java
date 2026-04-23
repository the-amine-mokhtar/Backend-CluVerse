package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CompetencyMatchingResponse {
    private String contextType;
    private String contextTitle;
    private Integer requestedSkills;
    private Integer candidatesEvaluated;
    private List<CompetencyMatchCandidateResponse> recommendations;
}
