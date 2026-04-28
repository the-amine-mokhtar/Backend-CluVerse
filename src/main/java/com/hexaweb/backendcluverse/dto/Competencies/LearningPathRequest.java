package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathRequest {
    private String skill_name;
    private int target_level;
}
