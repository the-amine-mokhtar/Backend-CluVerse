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
public class EventAiSchedulingDto {
    private String recommendationTitle;
    private String recommendationNarrative;
    private String forecastNarrative;
    private int predictionParticipants;
    private int expectedLift;
    private int confidence;
    private int successProbability;
    private String trendDirection;
    private int trendDeltaPercent;
    private int analyzedPastEvents;
    private EventAiSchedulingSlotDto bestDay;
    private EventAiSchedulingSlotDto bestHour;
    private EventAiSchedulingSlotDto bestMonth;
    private List<EventAiSchedulingSlotDto> topDays;
    private List<EventAiSchedulingSlotDto> topHours;
    private List<EventAiSchedulingTrendPointDto> historicalTrend;
    private List<EventAiSchedulingTrendPointDto> forecastTrend;
    private List<EventAiSchedulingTrendPointDto> monthlyTrend;
    private List<String> forecastHighlights;
}
