package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAiRankedEventDto {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String category;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer participantsCount;
    private Integer capacity;
    private Integer availableSeats;
    private String locationName;
    private String eventType;
    private String meetingUrl;
    private Long campaignId;
    private String campaignTitle;
    private EventAiInsightDto aiInsight;
}
