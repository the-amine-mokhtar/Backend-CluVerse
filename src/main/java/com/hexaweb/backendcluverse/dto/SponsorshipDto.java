package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.SponsorshipStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SponsorshipDto {
    private Long id;
    private Long sponsorId;
    private String sponsorName;
    private String sponsorLogoUrl;

    private Long eventId;
    private String eventName;
    private String ownerName;

    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal amount;
    private BigDecimal expectedAmount;
    private BigDecimal agreedAmount;
    private BigDecimal paidAmount;

    private String proposalSummary;
    private String proposalDocumentName;
    private String contractDocumentName;
    private String signedDocumentName;
    private String contractReference;
    private String notes;

    private LocalDateTime outreachSentAt;
    private String outreachDecision;
    private LocalDateTime proposalSentAt;
    private LocalDateTime contractSentAt;
    private LocalDateTime signedAt;
    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private SponsorshipStatus status;
}
