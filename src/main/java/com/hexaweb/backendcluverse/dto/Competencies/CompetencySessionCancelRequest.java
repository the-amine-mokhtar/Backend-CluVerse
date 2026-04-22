package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencySessionCancelRequest {
    private String reason;
}
