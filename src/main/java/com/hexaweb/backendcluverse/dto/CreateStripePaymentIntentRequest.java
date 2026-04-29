package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateStripePaymentIntentRequest {
    private Long amountCents;
    private String currency;
    private String sponsorName;
    private String sponsorEmail;
    private String sponsorPhone;
    private String reference;
}
