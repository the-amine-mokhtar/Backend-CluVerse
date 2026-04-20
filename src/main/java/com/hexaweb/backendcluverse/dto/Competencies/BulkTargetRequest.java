package com.hexaweb.backendcluverse.dto.Competencies;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkTargetRequest {

    @NotEmpty
    private List<Long> userIds;

    // Backward-compatible alias for competencyId.
    private Long skillId;

    // Preferred field name.
    private Long competencyId;

    @NotNull
    @Min(0)
    @Max(5)
    private Integer targetLevel;
}
