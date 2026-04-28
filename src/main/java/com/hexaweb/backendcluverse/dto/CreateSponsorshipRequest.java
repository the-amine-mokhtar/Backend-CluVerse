package com.hexaweb.backendcluverse.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateSponsorshipRequest {
    private Long sponsorId;
    private Long eventId;
    private String eventName;
    private BigDecimal expectedAmount;
    private String proposalSummary;
    private String notes;
}
