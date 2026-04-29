package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MemberCompetencyGapResponse {
    private Long id;
    private Integer currentLevel;
    private Integer targetLevel;
    private Integer gap;
}
