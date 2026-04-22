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
public class CompleteSponsorPaymentRequest {
    private BigDecimal amountEur;
    private BigDecimal amountTnd;
    private BigDecimal conversionRate;
    private String paymentIntentId;
    private String reference;
    private String sponsorPhone;
}
