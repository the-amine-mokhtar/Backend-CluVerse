package com.hexaweb.backendcluverse.services.competencies;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioSmsService {

    private final WebClient.Builder webClientBuilder;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    public boolean sendSms(String toPhoneNumber, String messageBody) {
        if (!twilioEnabled) {
            return false;
        }

        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromNumber) || isBlank(toPhoneNumber) || isBlank(messageBody)) {
            log.warn("Twilio SMS skipped: missing configuration or payload");
            return false;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", toPhoneNumber.trim());
            form.add("From", fromNumber.trim());
            form.add("Body", messageBody);

            webClientBuilder.baseUrl("https://api.twilio.com")
                    .build()
                    .post()
                    .uri("/2010-04-01/Accounts/{sid}/Messages.json", accountSid.trim())
                    .headers(headers -> headers.setBasicAuth(accountSid.trim(), authToken.trim()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(8));

            return true;
        } catch (Exception ex) {
            log.warn("Twilio SMS failed for {}: {}", toPhoneNumber, ex.getMessage());
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
