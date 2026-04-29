package com.hexaweb.backendcluverse.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateSponsorshipRequest {
    private String eventName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal expectedAmount;
    private BigDecimal agreedAmount;
    private BigDecimal paidAmount;
    private String proposalSummary;
    private String proposalDocumentName;
    private String contractDocumentName;
    private String signedDocumentName;
    private String contractReference;
    private String notes;
}
