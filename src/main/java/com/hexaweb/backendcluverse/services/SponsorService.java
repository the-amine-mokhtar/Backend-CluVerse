package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.enumerations.SponsorStatus;
import com.hexaweb.backendcluverse.entities.sponsoring.Sponsor;
import com.hexaweb.backendcluverse.repositories.SponsorRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SponsorService extends EntityServiceImpl<Sponsor, Long> {
    private final SponsorRepository sponsorRepository;
    private final JavaMailSender mailSender;
    private final CloudinaryService cloudinaryService;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${server.port}")
    private String serverPort;

    public SponsorService(SponsorRepository repository, JavaMailSender mailSender, CloudinaryService cloudinaryService) {
        super(repository);
        this.sponsorRepository = repository;
        this.mailSender = mailSender;
        this.cloudinaryService = cloudinaryService;
    }

    public Sponsor createPendingSponsor(Sponsor sponsor) {
        sponsor.setStatus(SponsorStatus.PENDING);
        sponsor.setConfirmationToken(UUID.randomUUID().toString());
        sponsor.setTokenExpiresAt(LocalDateTime.now().plusHours(72));
        Sponsor saved = sponsorRepository.save(sponsor);
        sendInvitationEmail(saved);
        return saved;
    }

    public Sponsor confirmByToken(String token) {
        Sponsor sponsor = sponsorRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid confirmation token"));

        if (sponsor.getStatus() != SponsorStatus.PENDING) {
            return sponsor;
        }

        if (isExpired(sponsor)) {
            sponsor.setStatus(SponsorStatus.DENIED);
            return sponsorRepository.save(sponsor);
        }

        sponsor.setStatus(SponsorStatus.CONFIRMED);
        sponsor.setJoinDate(LocalDate.now());
        sponsor.setConfirmationToken(null);
        sponsor.setTokenExpiresAt(null);
        return sponsorRepository.save(sponsor);
    }

    public Sponsor denyByToken(String token) {
        Sponsor sponsor = sponsorRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid confirmation token"));

        if (sponsor.getStatus() == SponsorStatus.PENDING) {
            sponsor.setStatus(SponsorStatus.DENIED);
            sponsor.setConfirmationToken(null);
            sponsor.setTokenExpiresAt(null);
            return sponsorRepository.save(sponsor);
        }

        return sponsor;
    }

    private boolean isExpired(Sponsor sponsor) {
        return sponsor.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(sponsor.getTokenExpiresAt());
    }

    public Sponsor uploadLogo(Long sponsorId, MultipartFile file) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));
        try {
            String logoUrl = cloudinaryService.uploadLogo(file);
            sponsor.setLogoUrl(logoUrl);
            return sponsorRepository.save(sponsor);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload sponsor logo", e);
        }
    }

    public void deleteWithReason(Long sponsorId, String reason) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));
        sendTerminationEmail(sponsor, reason);
        sponsorRepository.deleteById(sponsorId);
    }

    private void sendInvitationEmail(Sponsor sponsor) {
                String token = sponsor.getConfirmationToken();
                String confirmLink = "http://localhost:" + serverPort + "/api/sponsors/confirm?token=" + token;
                String denyLink = "http://localhost:" + serverPort + "/api/sponsors/deny?token=" + token;

                                String html = """
                                                                <div style="margin:0;padding:0;background:#090b1d;font-family:Inter,Segoe UI,Arial,sans-serif;">
                                                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#090b1d;padding:28px 14px;">
                                                                        <tr>
                                                                            <td align="center">
                                                                                <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="max-width:620px;width:100%%;background:#0f132b;border:1px solid #1f2a4a;border-radius:16px;overflow:hidden;">
                                                                                    <tr>
                                                                                        <td style="padding:22px 24px;background:linear-gradient(135deg,#111a3f 0%%,#0f132b 55%%,#0b1024 100%%);border-bottom:1px solid #1f2a4a;">
                                                                                            <div style="font-size:12px;letter-spacing:0.14em;text-transform:uppercase;color:#2dd4bf;font-weight:700;">Cluverse</div>
                                                                                            <h2 style="margin:10px 0 0;color:#ffffff;font-size:24px;line-height:1.25;">Sponsorship Invitation</h2>
                                                                                        </td>
                                                                                    </tr>
                                                                                    <tr>
                                                                                        <td style="padding:24px;color:#d8def7;font-size:15px;line-height:1.7;">
                                                                                            <p style="margin:0 0 12px;">Hello <strong style="color:#ffffff;">%s</strong>,</p>
                                                                                            <p style="margin:0 0 12px;">
                                                                                                We would be honored to have your support as a sponsor. Your contribution helps us
                                                                                                build impactful student activities, events, and opportunities.
                                                                                            </p>
                                                                                            <p style="margin:0 0 18px;">Please choose one of the options below:</p>

                                                                                            <table role="presentation" cellspacing="0" cellpadding="0" style="margin:22px 0 14px;">
                                                                                                <tr>
                                                                                                    <td style="padding-right:14px;padding-bottom:10px;">
                                                                                                        <a href="%s" style="display:inline-block;background:#2dd4bf;color:#07111f;text-decoration:none;padding:12px 18px;border-radius:10px;font-size:14px;font-weight:700;">Confirm Sponsorship</a>
                                                                                                    </td>
                                                                                                    <td style="padding-left:14px;padding-bottom:10px;">
                                                                                                        <a href="%s" style="display:inline-block;background:#e05c5c;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:10px;font-size:14px;font-weight:700;">Decline</a>
                                                                                                    </td>
                                                                                                </tr>
                                                                                            </table>

                                                                                            <div style="margin-top:8px;padding:10px 12px;background:#0b1024;border:1px solid #1f2a4a;border-radius:10px;font-size:13px;color:#93a3d9;">
                                                                                                This invitation expires in <strong style="color:#c7d2fe;">72 hours</strong>.
                                                                                            </div>
                                                                                        </td>
                                                                                    </tr>
                                                                                </table>
                                                                            </td>
                                                                        </tr>
                                                                    </table>
                                                                </div>
                                                                """.formatted(sponsor.getName(), confirmLink, denyLink);

                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                        helper.setFrom(fromAddress);
                        helper.setTo(sponsor.getContactEmail());
                        helper.setSubject("Sponsorship Invitation - " + sponsor.getName());
                        helper.setText(html, true);
                        mailSender.send(message);
                } catch (MessagingException e) {
                        throw new RuntimeException("Failed to send sponsor invitation email", e);
                }
    }

        private void sendTerminationEmail(Sponsor sponsor, String reason) {
                String html = """
                                <div style="margin:0;padding:0;background:#090b1d;font-family:Inter,Segoe UI,Arial,sans-serif;">
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#090b1d;padding:28px 14px;">
                                        <tr>
                                            <td align="center">
                                                <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="max-width:620px;width:100%%;background:#0f132b;border:1px solid #1f2a4a;border-radius:16px;overflow:hidden;">
                                                    <tr>
                                                        <td style="padding:22px 24px;background:linear-gradient(135deg,#111a3f 0%%,#0f132b 55%%,#0b1024 100%%);border-bottom:1px solid #1f2a4a;">
                                                            <div style="font-size:12px;letter-spacing:0.14em;text-transform:uppercase;color:#2dd4bf;font-weight:700;">Cluverse</div>
                                                            <h2 style="margin:10px 0 0;color:#ffffff;font-size:24px;line-height:1.25;">Sponsorship Closure Notice</h2>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding:24px;color:#d8def7;font-size:15px;line-height:1.7;">
                                                            <p style="margin:0 0 12px;">Hello <strong style="color:#ffffff;">%s</strong>,</p>
                                                            <p style="margin:0 0 12px;">
                                                                We appreciate your collaboration with our club. This message confirms that our sponsorship relationship has been closed.
                                                            </p>
                                                            <div style="margin:14px 0;padding:10px 12px;background:#0b1024;border:1px solid #1f2a4a;border-radius:10px;font-size:13px;color:#93a3d9;">
                                                                Reason: <strong style="color:#c7d2fe;">%s</strong>
                                                            </div>
                                                            <p style="margin:0;">Thank you for your time and support.</p>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                                """.formatted(sponsor.getName(), reason);

                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                        helper.setFrom(fromAddress);
                        helper.setTo(sponsor.getContactEmail());
                        helper.setSubject("Sponsorship closure - " + sponsor.getName());
                        helper.setText(html, true);
                        mailSender.send(message);
                } catch (MessagingException e) {
                        throw new RuntimeException("Failed to send sponsor closure email", e);
                }
        }
}

