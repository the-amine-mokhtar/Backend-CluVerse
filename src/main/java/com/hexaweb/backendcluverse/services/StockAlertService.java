package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.logistics.Resource;
import com.hexaweb.backendcluverse.enumerations.RoleType;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class StockAlertService {

    private static final Logger LOGGER = Logger.getLogger(StockAlertService.class.getName());

    private final JavaMailSender mailSender;
    private final MembershipRepository membershipRepository;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:4200}")
    private String frontendUrl;

    public StockAlertService(JavaMailSender mailSender, MembershipRepository membershipRepository) {
        this.mailSender = mailSender;
        this.membershipRepository = membershipRepository;
    }

    public void sendLowStockAlert(Resource resource, int threshold) {
        if (resource == null) {
            return;
        }

        String to = null;
        String clubName = null;
        try {
            // Resource has a Club relation (lazy). Within @Transactional service calls, this is safe.
            if (resource.getClub() != null) {
                to = resource.getClub().getEmail();
                clubName = resource.getClub().getName();
            }
        } catch (Exception ignored) {
            // Fallback below
        }

        if (to == null || to.trim().isEmpty()) {
            to = fromAddress;
        }

        String recipientName = resolveRecipientName(resource, clubName, to);
        String greetingName = (recipientName == null || recipientName.trim().isEmpty()) ? "" : (" " + safe(recipientName));
        String resourceLink = frontendUrl + "/resources/" + resource.getId();

       StringBuilder htmlBody = new StringBuilder();
htmlBody.append("<!DOCTYPE html>\n");
htmlBody.append("<html>\n");
htmlBody.append("<head>\n");
htmlBody.append("  <meta charset=\"UTF-8\">\n");
htmlBody.append("  <style>\n");
htmlBody.append("    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n");
htmlBody.append("    .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n");

htmlBody.append("    .alert-box { \n");
htmlBody.append("      background-color: #ecfeff; \n");  // light cyan
htmlBody.append("      border: 1px solid #06b6d4; \n");  // cyan matching button
htmlBody.append("      padding: 15px; \n");
htmlBody.append("      border-radius: 5px; \n");
htmlBody.append("      margin: 15px 0; \n");
htmlBody.append("    }\n");

htmlBody.append("    .info-item { margin: 10px 0; }\n");
htmlBody.append("    .button-container { text-align: center; margin-top: 20px; }\n");
htmlBody.append("    .button { background: linear-gradient(to right, #06b6d4, #059669); color: white; padding: 12px 24px; border-radius: 5px; text-decoration: none; display: inline-block; font-weight: bold; }\n");
htmlBody.append("    .button:hover { background: linear-gradient(to right, #0891b2, #047857); }\n");
htmlBody.append("    .footer { color: #000; font-size: 16px; margin-top: 20px; }\n");
htmlBody.append("  </style>\n");
htmlBody.append("</head>\n");
htmlBody.append("<body>\n");

htmlBody.append("  <div class=\"container\">\n");
htmlBody.append("    <h2>Bonjour").append(greetingName).append(",</h2>\n");
htmlBody.append("    <p>Nous vous informons que le niveau de stock de la ressource suivante est actuellement faible et nécessite une attention rapide :</p>\n");
htmlBody.append("\n");

htmlBody.append("    <div class=\"alert-box\">\n");
htmlBody.append("      <div class=\"info-item\"><b>Nom de la ressource :</b> ").append(safe(resource.getName())).append("</div>\n");
htmlBody.append("      <div class=\"info-item\"><b>Quantité disponible :</b> ").append(resource.getAvailableQuantity()).append(" unités</div>\n");
htmlBody.append("      <div class=\"info-item\"><b>Seuil d'alerte :</b> ").append(threshold).append(" unités</div>\n");
htmlBody.append("      <div class=\"info-item\"><b style=\"color: #059669;\">⚠️ Statut : Stock faible – action requise</b></div>\n");
htmlBody.append("    </div>\n");

htmlBody.append("\n");
htmlBody.append("    <p>Nous vous recommandons de procéder à un réapprovisionnement dans les plus brefs délais afin d'éviter toute rupture de stock pouvant impacter les opérations.</p>\n");
htmlBody.append("\n");

htmlBody.append("    <div class=\"button-container\">\n");
htmlBody.append("      <a href=\"").append(resourceLink).append("\" class=\"button\">✏️ Modifier la ressource</a>\n");
htmlBody.append("    </div>\n");

htmlBody.append("\n");
htmlBody.append("    <p>Merci de traiter cette alerte dès que possible.</p>\n");
htmlBody.append("\n");

htmlBody.append("    <div class=\"footer\">\n");
htmlBody.append("      <p>Cordialement,<br/>L’équipe Cluverse</p>\n");
htmlBody.append("    </div>\n");

htmlBody.append("  </div>\n");
htmlBody.append("</body>\n");
htmlBody.append("</html>\n");

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Alerte de stock faible - Réapprovisionnement requis");
            helper.setText(htmlBody.toString(), true);
            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            LOGGER.log(Level.WARNING,
                "Impossible d'envoyer l'alerte stock bas pour la ressource id=" + resource.getId() + ".",
                ex);
        } catch (MailException ex) {
            LOGGER.log(Level.WARNING,
                "Impossible d'envoyer l'alerte stock bas pour la ressource id=" + resource.getId() + ".",
                ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String resolveRecipientName(Resource resource, String clubName, String email) {
        if (resource == null || resource.getClub() == null || resource.getClub().getId() == null) {
            return fallbackName(clubName, email);
        }

        try {
            var memberships = membershipRepository.findByClubId(resource.getClub().getId());
            if (memberships != null && !memberships.isEmpty()) {
                Membership president = memberships.stream()
                    .filter(m -> m != null && m.isActive() && m.getRole() == RoleType.PRESIDENT)
                    .findFirst()
                    .orElse(null);

                Membership activeMember = memberships.stream()
                    .filter(m -> m != null && m.isActive())
                    .findFirst()
                    .orElse(null);

                User user = president != null ? president.getUser() : (activeMember != null ? activeMember.getUser() : null);
                String name = formatUserName(user);
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
            // Ignore and fallback
        }

        return fallbackName(clubName, email);
    }

    private String formatUserName(User user) {
        if (user == null) {
            return null;
        }
        String first = safe(user.getFirstName()).trim();
        String last = safe(user.getLastName()).trim();
        if (!first.isEmpty() && !last.isEmpty()) {
            return first + " " + last;
        }
        if (!first.isEmpty()) {
            return first;
        }
        if (!last.isEmpty()) {
            return last;
        }
        return null;
    }

    private String fallbackName(String clubName, String email) {
        if (clubName != null && !clubName.trim().isEmpty()) {
            return clubName.trim();
        }
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@')).replace('.', ' ');
        }
        return null;
    }
}
