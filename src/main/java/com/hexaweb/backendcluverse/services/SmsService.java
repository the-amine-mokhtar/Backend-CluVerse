package com.hexaweb.backendcluverse.services;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        logger.info("[SMS] Twilio initialized — from number: {}", fromNumber);
    }

    public boolean sendSms(String to, String messageBody) {
        if (to == null || to.isBlank()) {
            logger.warn("[SMS] Skipping — empty destination number");
            return false;
        }
        if (messageBody == null || messageBody.isBlank()) {
            logger.warn("[SMS] Skipping — empty message body");
            return false;
        }
        if (!to.startsWith("+")) {
            logger.warn("[SMS] Skipping — '{}' not in E.164 format", to);
            return false;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();

            logger.info("[SMS] ✅ Sent to {} — SID: {}", to, message.getSid());
            return true;

        } catch (ApiException e) {
            // ApiException expose getCode() uniquement (pas getStatus())
            logger.error("[SMS] ❌ Twilio API error sending to {}: [code={}] {}",
                    to, e.getCode(), e.getMessage());
            return false;

        } catch (TwilioException e) {
            logger.error("[SMS] ❌ Twilio error sending to {}: {}", to, e.getMessage());
            return false;

        } catch (Exception e) {
            logger.error("[SMS] ❌ Unexpected error sending to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }
}