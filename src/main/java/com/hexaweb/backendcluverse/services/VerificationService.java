package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service

public class VerificationService {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    private String generateActivationCode() {
        return UUID.randomUUID().toString();
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Clv@");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void sendActivationEmail(Club club) {
        String activationCode = generateActivationCode();
        String temporaryPassword = generateTemporaryPassword();

        club.setActivationCode(activationCode);
        club.setTemporaryPassword(temporaryPassword);
        club.setActivationExpiresAt(LocalDateTime.now().plusHours(24));
        clubRepository.save(club);
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(club.getEmail());
        mail.setSubject("Activation de votre club — " + club.getName());
        mail.setText(
                "Bonjour,\n\n" +
                        "Voici vos identifiants de connexion :\n\n" +
                        "Code : " + activationCode + "\n" +
                        "Mot de passe : " + temporaryPassword + "\n\n" +
                        "Cliquez ici pour activer votre compte :\n" +
                        baseUrl + "/verify?code=" + activationCode + "\n\n" +
                        "Ce lien expire dans 24h."
        );
        mailSender.send(mail);
    }
}
