package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.entities.MemberPayment;
import com.hexaweb.backendcluverse.entities.Notification;
import com.hexaweb.backendcluverse.enumerations.PaymentStatus;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.MemberPaymentRepository;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import com.hexaweb.backendcluverse.repositories.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberPaymentService extends EntityServiceImpl<MemberPayment, Long> {

    private final MemberPaymentRepository paymentRepository;
    private final MembershipRepository membershipRepository;
    private final ClubRepository clubRepository;
    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public MemberPaymentService(MemberPaymentRepository paymentRepository,
                                MembershipRepository membershipRepository,
                                ClubRepository clubRepository,
                                NotificationRepository notificationRepository,
                                JavaMailSender mailSender) {
        super(paymentRepository);
        this.paymentRepository = paymentRepository;
        this.membershipRepository = membershipRepository;
        this.clubRepository = clubRepository;
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    public List<MemberPayment> findByClub(Long clubId) {
        return paymentRepository.findByClub_Id(clubId);
    }

    public MemberPayment createPayment(Long membershipId, Long clubId, MemberPayment payment) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found"));
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));
        payment.setMembership(membership);
        payment.setClub(club);
        return paymentRepository.save(payment);
    }

    public int sendReminders(Long clubId) {
        List<MemberPayment> pending = paymentRepository.findByClub_IdAndStatus(clubId, PaymentStatus.PENDING);
        List<MemberPayment> overdue = paymentRepository.findByClub_IdAndStatus(clubId, PaymentStatus.OVERDUE);
        pending.addAll(overdue);

        for (MemberPayment p : pending) {
            String name = p.getMembership().getUser().getFirstName()
                    + " " + p.getMembership().getUser().getLastName();
            String email = p.getMembership().getUser().getEmail();
            String clubName = p.getClub().getName();
            boolean isOverdue = p.getStatus() == PaymentStatus.OVERDUE;

            // in-app notification
            Notification n = new Notification();
            n.setClubId(clubId);
            n.setCandidateName(name);
            n.setMessage("Due reminder: " + name + " (" + email + ") owes "
                    + p.getAmount() + " — due " + p.getDueDate() + " [" + p.getStatus() + "]");
            notificationRepository.save(n);

            // HTML email
            try {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
                helper.setFrom(fromAddress);
                helper.setTo(email);
                helper.setSubject((isOverdue ? "⚠️ Overdue Payment" : "🔔 Payment Reminder")
                        + " — " + clubName);
                helper.setText(buildReminderHtml(name, clubName, p, isOverdue), true);
                mailSender.send(mime);
            } catch (Exception ignored) {
                // don't fail the whole batch if one email bounces
            }
        }
        return pending.size();
    }

    private String buildReminderHtml(String memberName, String clubName, MemberPayment p, boolean isOverdue) {
        String bg      = "#0a0a1a";
        String card    = "#131329";
        String border  = "rgba(255,255,255,0.08)";
        String muted   = "#64748b";
        String sub     = "#94a3b8";
        String white   = "#f8fafc";
        String text    = "#e2e8f0";
        String accent  = isOverdue ? "#ef4444" : "#f59e0b";
        String label   = isOverdue ? "OVERDUE" : "PENDING";
        String icon    = isOverdue ? "&#9888;" : "&#128276;";
        int year       = java.time.Year.now().getValue();

        String amountFmt = String.format(java.util.Locale.US, "%.2f TND", p.getAmount());

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>" +
            "<body style=\"margin:0;padding:0;background:" + bg + ";font-family:'Segoe UI',Arial,sans-serif;color:" + text + ";\">" +
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + bg + ";padding:40px 20px;\">" +
            "<tr><td align=\"center\">" +
            "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;\">" +

            // Header
            "<tr><td style=\"padding-bottom:4px;\">" +
            "<p style=\"margin:0 0 6px;font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#475569;\">CLUVERSE FINANCE</p>" +
            "<h1 style=\"margin:0 0 4px;font-size:26px;font-weight:700;color:" + white + ";\">" +
            (isOverdue ? "Overdue Payment Notice" : "Payment Reminder") +
            "</h1>" +
            "<p style=\"margin:0;font-size:13px;color:" + muted + ";\">" + year + " &bull; " + clubName + "</p>" +
            "</td></tr>" +

            // Separator
            "<tr><td style=\"padding:16px 0 20px;\"><div style=\"height:1px;background:" + border + ";\"></div></td></tr>" +

            // Greeting
            "<tr><td style=\"padding-bottom:20px;\">" +
            "<p style=\"margin:0 0 10px;font-size:15px;\">Hello <strong>" + esc(memberName) + "</strong>,</p>" +
            "<p style=\"margin:0;color:" + sub + ";font-size:14px;line-height:1.7;\">" +
            (isOverdue
                ? "Your membership fee for <strong style=\"color:" + text + ";\">" + esc(clubName) + "</strong> is <strong style=\"color:" + accent + ";\">overdue</strong>. Please settle your balance as soon as possible to avoid suspension."
                : "This is a friendly reminder that your membership fee for <strong style=\"color:" + text + ";\">" + esc(clubName) + "</strong> is due soon. Please arrange payment before the due date.") +
            "</p>" +
            "</td></tr>" +

            // Payment card
            "<tr><td style=\"padding-bottom:24px;\">" +
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:12px;\">" +
            "<tr>" +
            "<td style=\"width:3px;background:" + accent + ";border-radius:3px 0 0 3px;\"></td>" +
            "<td style=\"background:" + card + ";border:1px solid " + border + ";border-left:none;border-radius:0 8px 8px 0;padding:20px;\">" +

            // Status badge row
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>" +
            "<td style=\"font-size:12px;color:" + muted + ";\">" +
            "<span style=\"color:" + accent + ";\">" + icon + "</span> Membership Fee" +
            "</td>" +
            "<td align=\"right\">" +
            "<span style=\"background:" + accent + ";color:#fff;padding:3px 10px;border-radius:20px;font-size:11px;font-weight:600;\">" + label + "</span>" +
            "</td>" +
            "</tr></table>" +

            // Amount
            "<p style=\"margin:14px 0 4px;font-size:28px;font-weight:700;color:" + white + ";\">" + esc(amountFmt) + "</p>" +

            // Details rows
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;border-top:1px solid " + border + ";padding-top:14px;\">" +
            "<tr><td style=\"font-size:13px;color:" + muted + ";padding:4px 0;\">Due date</td>" +
            "<td align=\"right\" style=\"font-size:13px;color:" + text + ";font-weight:600;padding:4px 0;\">" + esc(p.getDueDate().toString()) + "</td></tr>" +
            "<tr><td style=\"font-size:13px;color:" + muted + ";padding:4px 0;\">Club</td>" +
            "<td align=\"right\" style=\"font-size:13px;color:" + text + ";font-weight:600;padding:4px 0;\">" + esc(clubName) + "</td></tr>" +
            "<tr><td style=\"font-size:13px;color:" + muted + ";padding:4px 0;\">Status</td>" +
            "<td align=\"right\" style=\"padding:4px 0;\">" +
            "<span style=\"background:" + accent + ";color:#fff;padding:2px 8px;border-radius:12px;font-size:11px;font-weight:600;\">" + label + "</span>" +
            "</td></tr>" +
            "</table>" +

            "</td></tr></table>" +
            "</td></tr>" +

            // Footer
            "<tr><td style=\"border-top:1px solid " + border + ";padding-top:20px;\">" +
            "<p style=\"margin:0 0 12px;color:" + muted + ";font-size:13px;line-height:1.7;\">Log in to Cluverse to view your payment details and update your status.</p>" +
            "<p style=\"margin:0 0 8px;color:#475569;font-size:12px;\">This is an automated message from Cluverse. Do not reply to this email.</p>" +
            "<p style=\"margin:0;color:#334155;font-size:11px;\">&copy; " + year + " Cluverse &bull; Automated Financial Alerts</p>" +
            "</td></tr>" +

            "</table></td></tr></table></body></html>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
