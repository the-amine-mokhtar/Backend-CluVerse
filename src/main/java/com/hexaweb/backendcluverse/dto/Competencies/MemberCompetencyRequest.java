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

    // Backward-compatible alias for competencyId.
    private Long skillId;

    // Preferred field name for dashboard APIs.
    private Long competencyId;

    @NotNull
    @Min(0)
    @Max(5)
    private Integer currentLevel;

    @NotNull
    @Min(0)
    @Max(5)
    private Integer targetLevel;

    private UpdateSource lastUpdatedBy;
}
