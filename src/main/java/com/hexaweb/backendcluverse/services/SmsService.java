package com.hexaweb.backendcluverse.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.exception.TwilioException;
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
        logger.info("Twilio initialisé avec succès");
    }

    public boolean sendSms(String to, String messageBody) {
        try {
            // Validation des paramètres
            if (to == null || to.trim().isEmpty()) {
                logger.warn("Tentative d'envoi SMS avec numéro destinataire vide");
                return false;
            }
            
            if (messageBody == null || messageBody.trim().isEmpty()) {
                logger.warn("Tentative d'envoi SMS avec message vide");
                return false;
            }

            Message message = Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber(fromNumber),
                    messageBody
            ).create();

            logger.info("SMS envoyé avec succès à {}: SID={}", to, message.getSid());
            return true;

        } catch (TwilioException e) {
            logger.error("Erreur Twilio lors de l'envoi SMS à {}: {}", to, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'envoi SMS à {}: {}", to, e.getMessage(), e);
            return false;
        }
    }
}