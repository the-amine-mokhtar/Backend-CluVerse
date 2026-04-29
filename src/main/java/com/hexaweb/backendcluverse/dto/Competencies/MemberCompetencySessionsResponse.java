package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCompetencySessionsResponse {
    private MemberSessionMetricsResponse metrics;
    private List<CompetencySessionResponse> upcoming;
    private List<CompetencySessionResponse> past;
}
