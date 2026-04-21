package com.hexaweb.backendcluverse.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CampaignRequest {
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private double budget;
}