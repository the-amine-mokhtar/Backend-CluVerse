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
public class ParticipantAiInsightDto {
    private Long eventId;
    private int score;
    private int popularityScore;
    private int availabilityScore;
    private int recencyScore;
    private int trendScore;
    private int affinityScore;
    private String urgencyLevel;
    private String recommendationLabel;
    private String explanation;
    private String decisionMessage;
    private Integer seatsLeft;
    private List<String> badges;
}
