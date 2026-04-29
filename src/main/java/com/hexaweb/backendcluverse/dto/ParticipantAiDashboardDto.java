package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantAiDashboardDto {
    private int recommended;
    private int trending;
    private int urgent;
    private int profileSignals;
    private String profileSummary;
    private List<AiStatisticDto> statistics;
    private List<ParticipantAiRecommendationDto> recommendations;
    private List<ParticipantAiRecommendationDto> trendingEvents;
    private List<ParticipantAiRecommendationDto> urgentEvents;
}
