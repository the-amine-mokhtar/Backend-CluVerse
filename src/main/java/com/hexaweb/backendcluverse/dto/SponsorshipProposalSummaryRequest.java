package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SponsorshipProposalSummaryRequest {
    private String sponsorName;
    private String eventName;
    private BigDecimal expectedAmount;
}
