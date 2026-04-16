package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.enumerations.RoleType;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j

public class VerificationService {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    private String generateConnectionIdentifier(String clubName) {
        String digits = String.valueOf((int)(Math.random() * 900) + 100);
        String clubUpper = clubName.toUpperCase().replaceAll("\\s+", "");
        return digits + clubUpper + digits;
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
        String activationCode = generateConnectionIdentifier(club.getName());        String temporaryPassword = generateTemporaryPassword();

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
                        "Identifiant de connexion : " + activationCode + "\n" +
                        "Mot de passe : " + temporaryPassword + "\n\n" +
                        "Cliquez ici pour activer votre compte :\n" +
                        baseUrl + "/verify?code=" + activationCode + "\n\n" +
                        "Ce lien expire dans 24h."
        );
        try {
            mailSender.send(mail);
        } catch (MailException exception) {
            log.warn("Activation email could not be sent to {}: {}", club.getEmail(), exception.getMessage());
        }
    }


    public void sendMemberInvitationEmail(String email, Club club, String role) {
        String tempPassword = generateTemporaryPassword();
        String connectionIdentifier = generateConnectionIdentifier(club.getName());

        User user = new User();
        user.setEmail(email);
        user.setFirstName("Member");
        user.setLastName(club.getName());
        user.setPassword(BCrypt.hashpw(tempPassword, BCrypt.gensalt()));
        user.setConnectionIdentifier(connectionIdentifier);
        userRepository.save(user);

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setClub(club);
        membership.setRole(RoleType.valueOf(role));
        membership.setJoinDate(LocalDate.now());
        membership.setActive(true);
        membershipRepository.save(membership);

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(email);
        mail.setSubject("Invitation to join " + club.getName() + " on Cluverse");
        mail.setText(
                "Bonjour,\n\n" +
                        "You have been invited to join " + club.getName() + " as " + role + ".\n\n" +
                        "Your login credentials:\n\n" +
                        "Connection Identifier : " + connectionIdentifier + "\n" +
                        "Password : " + tempPassword + "\n\n" +
                        "Login at : " + baseUrl + "/auth/login\n\n"
        );
        try {
            mailSender.send(mail);
        } catch (MailException exception) {
            log.warn("Invitation email could not be sent to {}: {}", email, exception.getMessage());
        }
    }
}
