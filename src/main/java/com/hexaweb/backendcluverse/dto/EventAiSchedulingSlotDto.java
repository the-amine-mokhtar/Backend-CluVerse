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
public class EventAiSchedulingSlotDto {
    private String label;
    private String shortLabel;
    private int score;
    private int participants;
    private int fillRate;
    private int sampleSize;
}
