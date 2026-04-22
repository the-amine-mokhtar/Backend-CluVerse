package com.hexaweb.backendcluverse.dto.Competencies;

import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import com.hexaweb.backendcluverse.enumerations.UpdateSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MemberCompetencyResponse {
    private Long id;
    private Long userId;
    private String userName;

    // Backward-compatible / legacy field names.
    private Long skillId;
    private String skillName;
    private CompetencyType category;

    // Preferred dashboard field names.
    private Long competencyId;
    private String competencyName;
    private String competencyCategory;

    private Integer currentLevel;
    private Integer targetLevel;
    private Integer previousLevel;
    private Integer endorsementCount;

    // Backward-compatible and preferred gap names.
    private Integer gap;
    private Integer gapLevel;

    private UpdateSource lastUpdatedBy;
    private LocalDateTime lastUpdated;

    public MemberCompetencyResponse(Long id,
                                    Long userId,
                                    Long skillId,
                                    String skillName,
                                    CompetencyType category,
                                    Integer currentLevel,
                                    Integer targetLevel,
                                    Integer previousLevel,
                                    Integer endorsementCount,
                                    Integer gap,
                                    UpdateSource lastUpdatedBy,
                                    LocalDateTime lastUpdated) {
        this.id = id;
        this.userId = userId;
        this.skillId = skillId;
        this.competencyId = skillId;
        this.skillName = skillName;
        this.competencyName = skillName;
        this.category = category;
        this.competencyCategory = category == null ? null : category.name();
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
        this.previousLevel = previousLevel;
        this.endorsementCount = endorsementCount;
        this.gap = gap;
        this.gapLevel = gap;
        this.lastUpdatedBy = lastUpdatedBy;
        this.lastUpdated = lastUpdated;
    }
}
