package com.hexaweb.backendcluverse.dto.Competencies;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CompetencyMatchingRequest {

    @NotNull
    private Long clubId;

    @NotNull
    private String contextType;

    private String contextTitle;

    @NotEmpty
    private List<Long> requiredSkillIds;

    @Min(1)
    @Max(20)
    private Integer topN = 5;
}
