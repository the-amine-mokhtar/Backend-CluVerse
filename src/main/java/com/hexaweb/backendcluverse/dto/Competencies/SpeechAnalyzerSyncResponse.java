package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeechAnalyzerSyncResponse {
    private String sessionId;
    private double speechScore;
    private String speechLevel;
    private String feedback;
    private MemberCompetencyResponse memberCompetency;
}
