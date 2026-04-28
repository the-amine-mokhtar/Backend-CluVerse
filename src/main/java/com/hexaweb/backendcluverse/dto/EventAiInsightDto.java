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
public class EventAiInsightDto {
    private Long eventId;
    private int score;
    private int popularityScore;
    private int participationScore;
    private int recencyScore;
    private int trendScore;
    private int conversionScore;
    private String momentum;
    private String badgeLabel;
    private List<String> reasons;
}
