package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.logistics.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class StockAlertService {

    private static final Logger LOGGER = Logger.getLogger(StockAlertService.class.getName());

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public StockAlertService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendLowStockAlert(Resource resource, int threshold) {
        if (resource == null) {
            return;
        }

        String to = null;
        try {
            // Resource has a Club relation (lazy). Within @Transactional service calls, this is safe.
            if (resource.getClub() != null) {
                to = resource.getClub().getEmail();
            }
        } catch (Exception ignored) {
            // Fallback below
        }

        if (to == null || to.trim().isEmpty()) {
            to = fromAddress;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(to);
        mail.setSubject("Alerte stock bas — " + safe(resource.getName()));
        mail.setText(
            "Bonjour,\n\n" +
            "Alerte: la ressource \"" + safe(resource.getName()) + "\" est bientot en rupture.\n" +
            "Quantite disponible actuelle: " + resource.getAvailableQuantity() + "\n" +
            "Seuil d'alerte: " + threshold + "\n\n" +
            "Merci de planifier un re-approvisionnement.\n"
        );

        try {
            mailSender.send(mail);
        } catch (MailException ex) {
            LOGGER.log(Level.WARNING,
                "Impossible d'envoyer l'alerte stock bas pour la ressource id=" + resource.getId() + ".",
                ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
