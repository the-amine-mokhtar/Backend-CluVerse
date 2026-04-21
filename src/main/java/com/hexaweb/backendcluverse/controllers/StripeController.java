package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.CreateStripePaymentIntentRequest;
import com.hexaweb.backendcluverse.dto.CreateStripePaymentIntentResponse;
import com.hexaweb.backendcluverse.dto.StripePublicConfigResponse;
import com.hexaweb.backendcluverse.services.StripePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripePaymentService stripePaymentService;

    @GetMapping("/public-config")
    public StripePublicConfigResponse getPublicConfig() {
        return stripePaymentService.getPublicConfig();
    }

    @PostMapping("/create-payment-intent")
    public CreateStripePaymentIntentResponse createPaymentIntent(@RequestBody CreateStripePaymentIntentRequest request) {
        return stripePaymentService.createPaymentIntent(request);
    }
}
