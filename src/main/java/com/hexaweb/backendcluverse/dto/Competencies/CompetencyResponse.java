package com.hexaweb.backendcluverse.dto.Competencies;

import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyResponse {
    private Long id;
    private String name;
    private String description;
    private CompetencyType category;
    private Long clubId;
}
