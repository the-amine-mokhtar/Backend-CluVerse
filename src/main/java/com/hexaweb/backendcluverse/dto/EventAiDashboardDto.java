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
public class EventAiDashboardDto {
    private int avgScore;
    private int trending;
    private int popular;
    private int almostFull;
    private List<AiStatisticDto> statistics;
    private List<EventAiRankedEventDto> rankedEvents;
    private List<EventAiRankedEventDto> highlights;
    private List<EventAiRankedEventDto> trendingEvents;
    private List<EventAiRankedEventDto> almostFullEvents;
}
