package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.services.DonationReceiptEmailService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.publishable-key}")
    private String publishableKey;

    private final DonationReceiptEmailService receiptEmailService;

    public StripeController(DonationReceiptEmailService receiptEmailService) {
        this.receiptEmailService = receiptEmailService;
    }

    @GetMapping("/public-config")
    public Map<String, String> getPublicConfig() {
        return Map.of("publishableKey", publishableKey);
    }

    @PostMapping("/create-payment-intent")
    public Map<String, String> createPaymentIntent(@RequestBody PaymentIntentRequest req) {
        Stripe.apiKey = secretKey;
        try {
            PaymentIntent intent = PaymentIntent.create(
                PaymentIntentCreateParams.builder()
                    .setAmount((long) req.getAmountCents())
                    .setCurrency(req.getCurrency())
                    .putMetadata("sponsorName", nvl(req.getSponsorName()))
                    .putMetadata("sponsorEmail", nvl(req.getSponsorEmail()))
                    .putMetadata("sponsorPhone", nvl(req.getSponsorPhone()))
                    .putMetadata("reference",    nvl(req.getReference()))
                    .build()
            );
            return Map.of(
                "clientSecret",    intent.getClientSecret(),
                "paymentIntentId", intent.getId(),
                "publishableKey",  publishableKey
            );
        } catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/send-receipt")
    public Map<String, String> sendReceipt(@RequestBody ReceiptRequest req) {
        receiptEmailService.sendReceipt(new DonationReceiptEmailService.ReceiptData(
            req.getSponsorName(),
            req.getSponsorEmail(),
            req.getSponsorPhone(),
            req.getClubName(),
            req.getAmountEur(),
            req.getAmountTnd(),
            req.getReference(),
            req.getPaymentIntentId(),
            req.getDate()
        ));
        return Map.of("status", "sent");
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    @Getter @Setter
    public static class ReceiptRequest {
        private String sponsorName;
        private String sponsorEmail;
        private String sponsorPhone;
        private String clubName;
        private double amountEur;
        private double amountTnd;
        private String reference;
        private String paymentIntentId;
        private String date;
    }

    @Getter @Setter
    public static class PaymentIntentRequest {
        private int amountCents;
        private String currency;
        private String sponsorName;
        private String sponsorEmail;
        private String sponsorPhone;
        private String reference;
    }
}
