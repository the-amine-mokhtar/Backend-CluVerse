package com.hexaweb.backendcluverse.dto.Competencies;

import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompetencyRequest {

    @NotNull
    @NotBlank
    private String name;

    private String description;

    @NotNull
    private CompetencyType category;

    @NotNull
    private Long clubId;
}
