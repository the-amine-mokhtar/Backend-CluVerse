package com.hexaweb.backendcluverse.dto.Competencies;

import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import com.hexaweb.backendcluverse.enumerations.UpdateSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberCompetencyResponse {
    private Long id;
    private Long userId;
    private Long skillId;
    private String skillName;
    private CompetencyType category;
    private Integer currentLevel;
    private Integer targetLevel;
    private Integer previousLevel;
    private Integer endorsementCount;
    private Integer gap;
    private UpdateSource lastUpdatedBy;
    private LocalDateTime lastUpdated;
}
