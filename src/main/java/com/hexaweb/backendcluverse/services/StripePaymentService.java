package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.CreateStripePaymentIntentRequest;
import com.hexaweb.backendcluverse.dto.CreateStripePaymentIntentResponse;
import com.hexaweb.backendcluverse.dto.StripePublicConfigResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class StripePaymentService {

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.publishable-key:}")
    private String stripePublishableKey;

    public StripePublicConfigResponse getPublicConfig() {
        if (!hasText(stripePublishableKey)) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Stripe publishable key is not configured");
        }
        return new StripePublicConfigResponse(stripePublishableKey.trim());
    }

    public CreateStripePaymentIntentResponse createPaymentIntent(CreateStripePaymentIntentRequest request) {
        validateRequest(request);

        if (!hasText(stripeSecretKey)) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Stripe secret key is not configured");
        }

        Stripe.apiKey = stripeSecretKey.trim();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("sponsorName", safe(request.getSponsorName()));
        metadata.put("sponsorEmail", safe(request.getSponsorEmail()));
        metadata.put("sponsorPhone", safe(request.getSponsorPhone()));
        metadata.put("reference", safe(request.getReference()));

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmountCents())
                .setCurrency(request.getCurrency().trim().toLowerCase())
            .addPaymentMethodType("card")
                .putAllMetadata(metadata)
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params);
            return new CreateStripePaymentIntentResponse(
                    intent.getClientSecret(),
                    intent.getId(),
                    stripePublishableKey.trim()
            );
        } catch (StripeException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Failed to create Stripe payment intent: " + ex.getMessage());
        }
    }

    private void validateRequest(CreateStripePaymentIntentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "amountCents must be greater than 0");
        }
        if (!hasText(request.getCurrency())) {
            throw new ResponseStatusException(BAD_REQUEST, "currency is required");
        }
        if (!hasText(request.getSponsorName())) {
            throw new ResponseStatusException(BAD_REQUEST, "sponsorName is required");
        }
        if (!hasText(request.getSponsorEmail())) {
            throw new ResponseStatusException(BAD_REQUEST, "sponsorEmail is required");
        }
        if (!hasText(stripePublishableKey)) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Stripe publishable key is not configured");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
