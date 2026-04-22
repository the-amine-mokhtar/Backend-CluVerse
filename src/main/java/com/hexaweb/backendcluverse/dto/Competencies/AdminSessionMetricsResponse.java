package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSessionMetricsResponse {
    private long totalSessions;
    private long upcomingSessions;
    private long completedThisMonth;
    private long cancelledSessions;
}
