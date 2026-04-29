package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.PasswordResetToken;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.PasswordResetTokenRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.base-url}")
    private String baseUrl;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findFirstByEmailOrderByIdDesc(email).orElse(null);
        // Always return success — never reveal whether email exists
        if (user == null) return;

        String token = UUID.randomUUID().toString();
        tokenRepository.save(new PasswordResetToken(token, user));

        String resetLink = baseUrl + "/auth/reset-password?token=" + token;
        sendResetEmail(user, resetLink);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset link."));

        if (resetToken.isUsed()) throw new RuntimeException("This reset link has already been used.");
        if (resetToken.isExpired()) throw new RuntimeException("This reset link has expired. Please request a new one.");

        User user = resetToken.getUser();
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private void sendResetEmail(User user, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("Reset your Cluverse password");
            helper.setText(buildEmailBody(user, resetLink), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }

    private String buildEmailBody(User user, String resetLink) {
        String name = (user.getFirstName() != null ? user.getFirstName() : "there");
        int year = java.time.LocalDateTime.now().getYear();
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;\">"
            + "<div style=\"max-width:560px;margin:32px auto;\">"
            + "<div style=\"background:linear-gradient(135deg,#1d4ed8 0%,#0f172a 100%);color:#fff;padding:32px 28px;border-radius:14px 14px 0 0;\">"
            + "<div style=\"font-size:11px;font-weight:700;letter-spacing:3px;opacity:0.75;margin-bottom:10px;\">CLUVERSE</div>"
            + "<div style=\"font-size:26px;font-weight:700;margin-bottom:4px;\">Password Reset</div>"
            + "<div style=\"font-size:13px;opacity:0.8;\">This link expires in 1 hour</div>"
            + "</div>"
            + "<div style=\"background:#fff;padding:28px;border-radius:0 0 14px 14px;box-shadow:0 4px 24px rgba(0,0,0,0.07);\">"
            + "<p style=\"font-size:16px;color:#1e293b;margin-top:0;\">Hello <b>" + escapeHtml(name) + "</b>,</p>"
            + "<p style=\"font-size:14px;color:#475569;line-height:1.7;\">We received a request to reset your Cluverse password. Click the button below to choose a new password.</p>"
            + "<div style=\"text-align:center;margin:28px 0;\">"
            + "<a href=\"" + resetLink + "\" style=\"background:#1d4ed8;color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-weight:700;font-size:15px;display:inline-block;\">Reset Password</a>"
            + "</div>"
            + "<p style=\"font-size:13px;color:#94a3b8;\">If you did not request a password reset, you can safely ignore this email. The link will expire in 1 hour.</p>"
            + "</div>"
            + "<div style=\"text-align:center;padding:16px;color:#94a3b8;font-size:12px;\">"
            + "<p style=\"margin:0;\">&copy; " + year + " Cluverse &bull; Automated notification</p>"
            + "</div>"
            + "</div></body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
