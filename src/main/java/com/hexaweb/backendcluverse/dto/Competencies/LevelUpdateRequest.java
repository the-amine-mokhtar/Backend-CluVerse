package com.hexaweb.backendcluverse.dto.Competencies;

import com.hexaweb.backendcluverse.enumerations.UpdateSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LevelUpdateRequest {

    @NotNull
    @Min(0)
    @Max(5)
    private Integer newLevel;

    private UpdateSource source;
}
