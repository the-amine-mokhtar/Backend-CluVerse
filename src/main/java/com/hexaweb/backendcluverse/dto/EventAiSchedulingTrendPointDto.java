package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAiSchedulingTrendPointDto {
    private String label;
    private String shortLabel;
    private int participants;
    private int fillRate;
    private int score;
    private boolean projected;
    private int changeRate;
}
