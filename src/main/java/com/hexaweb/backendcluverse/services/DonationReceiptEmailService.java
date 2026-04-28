package com.hexaweb.backendcluverse.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DonationReceiptEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public DonationReceiptEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public record ReceiptData(
        String sponsorName,
        String sponsorEmail,
        String sponsorPhone,
        String clubName,
        double amountEur,
        double amountTnd,
        String reference,
        String paymentIntentId,
        String date
    ) {}

    public void sendReceipt(ReceiptData data) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(data.sponsorEmail().trim());
            helper.setSubject("Donation Receipt – " + data.reference() + " | Cluverse");
            helper.setText(buildHtml(data), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send donation receipt: " + e.getMessage());
        }
    }

    private String buildHtml(ReceiptData d) {
        String name      = esc(d.sponsorName());
        String club      = esc(d.clubName());
        String ref       = esc(d.reference());
        String piId      = esc(d.paymentIntentId());
        String phone     = (d.sponsorPhone() != null && !d.sponsorPhone().isBlank()) ? esc(d.sponsorPhone()) : "—";
        String amtEur    = String.format("%.2f EUR", d.amountEur());
        String amtTnd    = String.format("%.3f TND", d.amountTnd());
        String dateStr   = formatDate(d.date());
        int year         = LocalDate.now().getYear();

        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">"
            + "<title>Donation Receipt</title></head>"
            + "<body style=\"margin:0;padding:0;background:#f1f5f9;\">"
            + "<div style=\"font-family:'Segoe UI',Tahoma,Arial,sans-serif;max-width:620px;margin:32px auto;\">"

            // ── Header ──────────────────────────────────────────────────────
            + "<div style=\"background:linear-gradient(135deg,#1d4ed8 0%,#0f172a 100%);color:#ffffff;padding:32px 28px;border-radius:14px 14px 0 0;\">"
            + "<div style=\"font-size:11px;font-weight:700;letter-spacing:3px;opacity:0.7;margin-bottom:10px;\">CLUVERSE</div>"
            + "<div style=\"font-size:28px;font-weight:700;margin-bottom:6px;\">Donation Receipt</div>"
            + "<div style=\"font-size:13px;opacity:0.8;\">Reference: <b>" + ref + "</b> &bull; " + dateStr + "</div>"
            + "</div>"

            // ── Body ─────────────────────────────────────────────────────────
            + "<div style=\"background:#ffffff;padding:28px;border-radius:0 0 14px 14px;box-shadow:0 4px 24px rgba(0,0,0,0.07);\">"

            // greeting
            + "<p style=\"font-size:16px;color:#1e293b;margin-top:0;\">Hello <b>" + name + "</b>,</p>"
            + "<p style=\"font-size:14px;color:#475569;line-height:1.7;margin-bottom:24px;\">"
            + "Thank you for your generous donation to <b>" + club + "</b>! "
            + "Your payment was processed successfully via Stripe. "
            + "Please keep this email as your official receipt."
            + "</p>"

            // ── Amount highlight ──────────────────────────────────────────────
            + "<div style=\"background:linear-gradient(135deg,#eff6ff,#dbeafe);border:1px solid #bfdbfe;border-radius:12px;padding:20px 24px;margin-bottom:24px;text-align:center;\">"
            + "<div style=\"font-size:12px;color:#3b82f6;font-weight:700;letter-spacing:2px;margin-bottom:6px;\">AMOUNT DONATED</div>"
            + "<div style=\"font-size:36px;font-weight:800;color:#1d4ed8;\">" + amtEur + "</div>"
            + "<div style=\"font-size:13px;color:#64748b;margin-top:4px;\">Recorded as <b>" + amtTnd + "</b> in club treasury</div>"
            + "</div>"

            // ── Details table ─────────────────────────────────────────────────
            + "<h2 style=\"font-size:14px;font-weight:700;color:#0f172a;margin-bottom:12px;padding-bottom:8px;border-bottom:2px solid #f1f5f9;\">Transaction Details</h2>"
            + "<table style=\"width:100%;border-collapse:collapse;font-size:13px;\">"
            + row("Donor",           name)
            + row("Email",           esc(d.sponsorEmail()))
            + row("Phone",           phone)
            + row("Club",            club)
            + row("Date",            dateStr)
            + row("Reference",       ref)
            + row("Stripe Payment ID", piId)
            + "</table>"

            // ── Badge ─────────────────────────────────────────────────────────
            + "<div style=\"margin-top:24px;padding:14px 18px;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;display:flex;align-items:center;gap:10px;\">"
            + "<span style=\"font-size:22px;\">&#10003;</span>"
            + "<div>"
            + "<div style=\"font-size:13px;font-weight:700;color:#166534;\">Payment Confirmed</div>"
            + "<div style=\"font-size:12px;color:#15803d;margin-top:2px;\">Your donation has been securely processed and recorded.</div>"
            + "</div>"
            + "</div>"

            // ── CTA ───────────────────────────────────────────────────────────
            + "<div style=\"text-align:center;margin-top:28px;padding-top:20px;border-top:1px solid #f1f5f9;\">"
            + "<p style=\"font-size:13px;color:#64748b;\">If you have any questions about your donation, please contact us through the Cluverse platform.</p>"
            + "</div>"

            + "</div>"

            // ── Footer ────────────────────────────────────────────────────────
            + "<div style=\"text-align:center;padding:20px;color:#94a3b8;font-size:12px;line-height:1.6;\">"
            + "<p style=\"margin:0;\">This is an automated receipt from <b>Cluverse</b>. Do not reply to this email.</p>"
            + "<p style=\"margin:4px 0 0;opacity:0.7;\">&copy; " + year + " Cluverse &bull; Secure Donation Platform</p>"
            + "</div>"

            + "</div>"
            + "</body></html>";
    }

    private String row(String label, String value) {
        return "<tr>"
            + "<td style=\"padding:9px 12px 9px 0;color:#64748b;border-bottom:1px solid #f1f5f9;width:45%;vertical-align:top;\">" + label + "</td>"
            + "<td style=\"padding:9px 0;font-weight:600;color:#0f172a;border-bottom:1px solid #f1f5f9;\">" + value + "</td>"
            + "</tr>";
    }

    private String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String formatDate(String iso) {
        try {
            LocalDate d = LocalDate.parse(iso);
            return d.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        } catch (Exception e) {
            return iso != null ? iso : LocalDate.now().toString();
        }
    }
}
