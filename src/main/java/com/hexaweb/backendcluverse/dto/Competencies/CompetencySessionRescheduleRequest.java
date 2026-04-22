package com.hexaweb.backendcluverse.dto.Competencies;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencySessionRescheduleRequest {

    @NotNull
    private LocalDateTime startsAt;
}
