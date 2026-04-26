package com.hexaweb.backendcluverse.controllers;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @GetMapping("/public-config")
    public ResponseEntity<Map<String, String>> getPublicConfig() {
        return ResponseEntity.ok(Map.of("publishableKey", stripePublishableKey));
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody CreatePaymentIntentRequest request) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((long) request.amountCents())
                    .setCurrency(request.currency())
                    .addPaymentMethodType("card")
                    .putMetadata("sponsorName", request.sponsorName())
                    .putMetadata("sponsorEmail", request.sponsorEmail())
                    .putMetadata("sponsorPhone", request.sponsorPhone() != null ? request.sponsorPhone() : "")
                    .putMetadata("reference", request.reference())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return ResponseEntity.ok(Map.of(
                    "clientSecret", intent.getClientSecret(),
                    "paymentIntentId", intent.getId(),
                    "publishableKey", stripePublishableKey
            ));
        } catch (Throwable e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
        }
    }

    @PostMapping("/send-receipt")
    public ResponseEntity<Map<String, String>> sendReceipt(@RequestBody DonateReceiptRequest r) {
        try {
            String dateFormatted = formatDate(r.date());
            String amountEurStr = String.format(Locale.FRENCH, "%.2f EUR", r.amountEur());
            String amountTndStr = String.format(Locale.FRENCH, "%.3f TND", r.amountTnd());
            String phone = (r.sponsorPhone() != null && !r.sponsorPhone().isBlank()) ? r.sponsorPhone() : "—";

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(r.sponsorEmail());
            helper.setSubject("Donation Receipt — " + r.reference() + " | Cluverse");
            helper.setText(buildReceiptHtml(r, dateFormatted, amountEurStr, amountTndStr, phone), true);
            mailSender.send(mime);

            return ResponseEntity.ok(Map.of("status", "sent"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private String formatDate(String isoDate) {
        try {
            return LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
        } catch (Exception e) {
            return isoDate;
        }
    }

    private String buildReceiptHtml(DonateReceiptRequest r, String dateFormatted, String amountEur, String amountTnd, String phone) {
        String bg = "#0a0a1a";
        String card = "#131329";
        String border = "rgba(255,255,255,0.08)";
        String cyan = "#06b6d4";
        String muted = "#64748b";
        String sub = "#94a3b8";
        String white = "#f8fafc";
        String text = "#e2e8f0";

        String row = "<tr>" +
                "<td style=\"padding:11px 0;border-bottom:1px solid " + border + ";color:" + muted + ";font-size:13px;width:42%%;\">{L}</td>" +
                "<td style=\"padding:11px 0;border-bottom:1px solid " + border + ";color:{VC};font-size:13px;\">{V}</td>" +
                "</tr>";

        String rows =
            row.replace("{L}", "Donor").replace("{VC}", text).replace("{V}", esc(r.sponsorName())) +
            row.replace("{L}", "Email").replace("{VC}", cyan).replace("{V}", esc(r.sponsorEmail())) +
            row.replace("{L}", "Phone").replace("{VC}", text).replace("{V}", esc(phone)) +
            row.replace("{L}", "Club").replace("{VC}", cyan).replace("{V}", esc(r.clubName())) +
            row.replace("{L}", "Date").replace("{VC}", text).replace("{V}", esc(dateFormatted)) +
            row.replace("{L}", "Reference").replace("{VC}", text).replace("{V}", esc(r.reference())) +
            "<tr>" +
            "<td style=\"padding:11px 0;color:" + muted + ";font-size:13px;\">Stripe Payment ID</td>" +
            "<td style=\"padding:11px 0;color:" + cyan + ";font-size:13px;word-break:break-all;\">" + esc(r.paymentIntentId()) + "</td>" +
            "</tr>";

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>" +
            "<body style=\"margin:0;padding:0;background:" + bg + ";font-family:'Segoe UI',Arial,sans-serif;color:" + text + ";\">" +
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + bg + ";padding:40px 20px;\">" +
            "<tr><td align=\"center\">" +
            "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;\">" +

            // Header
            "<tr><td style=\"padding-bottom:8px;\">" +
            "<h1 style=\"margin:0;font-size:26px;font-weight:700;color:" + white + ";\">Donation Receipt</h1>" +
            "<p style=\"margin:6px 0 0;color:" + muted + ";font-size:13px;\">Reference: " + esc(r.reference()) + " &bull; " + esc(dateFormatted) + "</p>" +
            "</td></tr>" +

            // Separator
            "<tr><td style=\"padding:0 0 20px;\"><div style=\"height:1px;background:" + border + ";\"></div></td></tr>" +

            // Greeting
            "<tr><td style=\"padding-bottom:20px;\">" +
            "<p style=\"margin:0 0 10px;font-size:15px;\">Hello <strong>" + esc(r.sponsorName()) + "</strong>,</p>" +
            "<p style=\"margin:0;color:" + sub + ";font-size:14px;line-height:1.7;\">Thank you for your generous donation to <strong style=\"color:" + text + ";\">" + esc(r.clubName()) + "</strong>! Your payment was processed successfully via Stripe. Please keep this email as your official receipt.</p>" +
            "</td></tr>" +

            // Amount box
            "<tr><td style=\"padding-bottom:24px;\">" +
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + card + ";border:1px solid " + border + ";border-radius:12px;\">" +
            "<tr><td style=\"padding:32px;text-align:center;\">" +
            "<p style=\"margin:0 0 10px;font-size:11px;letter-spacing:2px;color:" + cyan + ";text-transform:uppercase;font-weight:600;\">Amount Donated</p>" +
            "<p style=\"margin:0;font-size:44px;font-weight:700;color:" + cyan + ";letter-spacing:-1px;\">" + amountEur + "</p>" +
            "<p style=\"margin:10px 0 0;font-size:13px;color:" + muted + ";\">Recorded as <strong style=\"color:" + sub + ";\">" + amountTnd + "</strong> in club treasury</p>" +
            "</td></tr></table>" +
            "</td></tr>" +

            // Transaction details
            "<tr><td style=\"padding-bottom:24px;\">" +
            "<h3 style=\"margin:0 0 16px;font-size:15px;font-weight:600;color:" + white + ";\">Transaction Details</h3>" +
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" + rows + "</table>" +
            "</td></tr>" +

            // Confirmation
            "<tr><td style=\"padding-bottom:24px;\">" +
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:rgba(34,197,94,0.08);border:1px solid rgba(34,197,94,0.25);border-radius:8px;\">" +
            "<tr><td style=\"padding:16px 20px;\">" +
            "<p style=\"margin:0;color:#22c55e;font-weight:600;font-size:14px;\">&#10003; Payment Confirmed</p>" +
            "<p style=\"margin:5px 0 0;color:" + sub + ";font-size:13px;\">Your donation has been securely processed and recorded.</p>" +
            "</td></tr></table>" +
            "</td></tr>" +

            // Footer
            "<tr><td style=\"border-top:1px solid " + border + ";padding-top:20px;\">" +
            "<p style=\"margin:0 0 12px;color:" + muted + ";font-size:13px;line-height:1.7;\">If you have any questions about your donation, please contact us through the <span style=\"color:" + cyan + ";\">Cluverse platform</span>.</p>" +
            "<p style=\"margin:0;color:#475569;font-size:12px;\">This is an automated receipt from Cluverse. Do not reply to this email.</p>" +
            "</td></tr>" +

            "</table></td></tr></table></body></html>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public record CreatePaymentIntentRequest(
            int amountCents,
            String currency,
            String sponsorName,
            String sponsorEmail,
            String sponsorPhone,
            String reference
    ) {}

    public record DonateReceiptRequest(
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
}
