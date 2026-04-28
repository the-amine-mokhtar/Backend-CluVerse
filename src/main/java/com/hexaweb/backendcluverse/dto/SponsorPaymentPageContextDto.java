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
public class SponsorPaymentPageContextDto {
    private Long sponsorshipId;
    private String sponsorName;
    private String sponsorEmail;
    private String eventName;
    private BigDecimal agreedAmount;
    private BigDecimal paidAmount;
    private String currency;
}
