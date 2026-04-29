package com.hexaweb.backendcluverse.dto.Competencies;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    // Primary field names - backward-compatible via @JsonAlias
    @JsonAlias({"skillId", "skill_id"})
    private Long competencyId;
    
    @JsonAlias({"skillName", "skill_name"})
    private String competencyName;
    
    @JsonAlias({"category"})
    private CompetencyType competencyCategory;

    private Integer currentLevel;
    private Integer targetLevel;
    private Integer previousLevel;
    private Integer endorsementCount;

    // Primary gap field - backward-compatible via @JsonAlias
    @JsonAlias({"gap"})
    private Integer gapLevel;

    private UpdateSource lastUpdatedBy;
    private LocalDateTime lastUpdated;
    
    // Backward-compatible properties for deserialization
    public void setSkillId(Long skillId) { this.competencyId = skillId; }
    public Long getSkillId() { return this.competencyId; }
    
    public void setSkillName(String skillName) { this.competencyName = skillName; }
    public String getSkillName() { return this.competencyName; }
    
    public void setCategory(CompetencyType category) { this.competencyCategory = category; }
    public CompetencyType getCategory() { return this.competencyCategory; }
    
    public void setGap(Integer gap) { this.gapLevel = gap; }
    public Integer getGap() { return this.gapLevel; }

    public MemberCompetencyResponse(Long id,
                                    Long userId,
                                    Long competencyId,
                                    String competencyName,
                                    CompetencyType competencyCategory,
                                    Integer currentLevel,
                                    Integer targetLevel,
                                    Integer previousLevel,
                                    Integer endorsementCount,
                                    Integer gapLevel,
                                    UpdateSource lastUpdatedBy,
                                    LocalDateTime lastUpdated) {
        this.id = id;
        this.userId = userId;
        this.competencyId = competencyId;
        this.competencyName = competencyName;
        this.competencyCategory = competencyCategory;
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
        this.previousLevel = previousLevel;
        this.endorsementCount = endorsementCount;
        this.gapLevel = gapLevel;
        this.lastUpdatedBy = lastUpdatedBy;
        this.lastUpdated = lastUpdated;
    }
}
