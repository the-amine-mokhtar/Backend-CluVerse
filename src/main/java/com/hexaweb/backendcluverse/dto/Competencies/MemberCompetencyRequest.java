package com.hexaweb.backendcluverse.dto.Competencies;

import com.hexaweb.backendcluverse.enumerations.UpdateSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MemberCompetencyRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long skillId;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer currentLevel;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer targetLevel;

    private UpdateSource lastUpdatedBy;
}
