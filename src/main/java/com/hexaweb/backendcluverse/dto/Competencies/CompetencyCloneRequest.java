package com.hexaweb.backendcluverse.dto.Competencies;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompetencyCloneRequest {

    @NotNull
    private Long targetClubId;
}