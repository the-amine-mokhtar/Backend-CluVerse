package com.hexaweb.backendcluverse.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.hexaweb.backendcluverse.controllers.NotificationController.BudgetAlertEmailRequest;
import com.hexaweb.backendcluverse.controllers.NotificationController.BudgetAlertItem;

@Service
public class BudgetAlertEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public BudgetAlertEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendBudgetAlertEmail(BudgetAlertEmailRequest request) {
        try {
            String highestLevel = getHighestSeverity(request.alerts());
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(request.recipientEmail().trim());
            helper.setSubject(buildSubject(request, highestLevel));
            helper.setText(buildBody(request, highestLevel), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send HTML budget alert email: " + e.getMessage());
        }
    }

    private String buildSubject(BudgetAlertEmailRequest request, String highestLevel) {
        int alertCount = request.alerts() == null ? 0 : request.alerts().size();
        String clubName = safeText(request.clubName(), "your club");
        String emoji = "Warning".equalsIgnoreCase(highestLevel) ? "⚠️" : "🚨";
        String plural = alertCount == 1 ? "alert" : "alerts";
        return emoji + " " + highestLevel.toUpperCase() + " — " + alertCount + " budget " + plural + " for " + clubName;
    }

    private String buildBody(BudgetAlertEmailRequest request, String highestLevel) {
        String recipientName = safeText(request.recipientName(), "there");
        String clubName = safeText(request.clubName(), "your club");
        String triggeredAt = safeText(request.triggeredAt(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm")));
        String exerciseYear = request.exerciseYear() == null ? "N/A" : String.valueOf(request.exerciseYear());
        List<BudgetAlertItem> alerts = request.alerts() == null ? List.of() : request.alerts();
        int alertCount = alerts.size();

        boolean isSevereOverall = !"Warning".equalsIgnoreCase(highestLevel);
        String headerGradient = isSevereOverall
                ? "linear-gradient(135deg, #be123c 0%, #7f1d1d 100%)"
                : "linear-gradient(135deg, #1d4ed8 0%, #0f172a 100%)";
        String summaryAccent = isSevereOverall ? "#dc2626" : "#d97706";

        StringBuilder alertsBlock = new StringBuilder();
        for (BudgetAlertItem alert : alerts) {
            String title = safeText(alert.title(), "Unknown budget");
            String department = safeText(alert.department(), "Club-wide");
            String level = normalizeLevel(alert.level());
            int utilization = alert.utilization() != null ? alert.utilization() : 0;
            int threshold = alert.reachedThreshold() != null? alert.reachedThreshold() : 0;
            int clampedUtil = Math.min(utilization, 100);

            AlertStyle style = getAlertStyle(level);

            alertsBlock
                .append("<div style=\"background:#ffffff;border:1px solid #e2e8f0;border-left:4px solid ").append(style.borderColor()).append(";border-radius:10px;padding:20px;margin-bottom:14px;\">")
                .append("<div style=\"display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:14px;\">")
                .append("<div>")
                .append("<div style=\"font-size:12px;color:#94a3b8;margin-bottom:4px;\">").append(style.icon()).append(" ").append(escapeHtml(department)).append("</div>")
                .append("<div style=\"font-size:16px;font-weight:700;color:#0f172a;\">").append(escapeHtml(title)).append("</div>")
                .append("</div>")
                .append("<span style=\"background:").append(style.badgeBg()).append(";color:").append(style.badgeColor()).append(";font-size:11px;font-weight:700;padding:4px 12px;border-radius:20px;white-space:nowrap;letter-spacing:0.3px;\">")
                .append(level.toUpperCase()).append("</span>")
                .append("</div>")
                .append("<div style=\"margin-bottom:10px;\">")
                .append("<div style=\"display:flex;justify-content:space-between;font-size:13px;margin-bottom:5px;\">")
                .append("<span style=\"color:#64748b;\">Budget consumed</span>")
                .append("<span style=\"font-weight:700;color:").append(style.borderColor()).append(";\">").append(utilization).append("%</span>")
                .append("</div>")
                .append("<div style=\"background:#f1f5f9;border-radius:6px;height:10px;overflow:hidden;\">")
                .append("<div style=\"background:").append(style.barColor()).append(";width:").append(clampedUtil).append("%;height:10px;border-radius:6px;\"></div>")
                .append("</div>")
                .append("</div>")
                .append("<div style=\"font-size:12px;color:#94a3b8;\">Alert threshold: <b style=\"color:#64748b;\">").append(threshold).append("%</b></div>")
                .append("</div>");
        }

        int currentYear = LocalDateTime.now().getYear();

        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"><title>Budget Alert</title></head>"
            + "<body style=\"margin:0;padding:0;background:#f1f5f9;\">"
            + "<div style=\"font-family:'Segoe UI',Tahoma,Arial,sans-serif;max-width:620px;margin:32px auto;\">"

            // ── Header ──────────────────────────────────────────────────────
            + "<div style=\"background:" + headerGradient + ";color:#ffffff;padding:32px 28px;border-radius:14px 14px 0 0;\">"
            + "<div style=\"font-size:11px;font-weight:700;letter-spacing:3px;opacity:0.75;margin-bottom:10px;\">CLUVERSE FINANCE</div>"
            + "<div style=\"font-size:28px;font-weight:700;margin-bottom:6px;\">Budget Alert</div>"
            + "<div style=\"font-size:13px;opacity:0.8;\">" + escapeHtml(exerciseYear) + " &bull; Detected " + escapeHtml(triggeredAt) + "</div>"
            + "</div>"

            // ── Body ─────────────────────────────────────────────────────────
            + "<div style=\"background:#ffffff;padding:28px;border-radius:0 0 14px 14px;box-shadow:0 4px 24px rgba(0,0,0,0.07);\">"
            + "<p style=\"font-size:16px;color:#1e293b;margin-top:0;\">Hello <b>" + escapeHtml(recipientName) + "</b>,</p>"
            + "<p style=\"font-size:14px;color:#475569;line-height:1.7;margin-bottom:20px;\">"
            + "<b>" + alertCount + " budget " + (alertCount == 1 ? "alert" : "alerts") + "</b> "
            + (alertCount == 1 ? "has" : "have") + " been triggered for <b>" + escapeHtml(clubName) + "</b>. "
            + "Please review the details below and take corrective action if needed to maintain financial stability."
            + "</p>"

            // ── Summary strip ────────────────────────────────────────────────
            + "<table style=\"width:100%;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:14px 18px;margin-bottom:24px;font-size:13px;border-collapse:collapse;\"><tr>"
            + "<td style=\"padding:8px 16px 8px 0;\"><div style=\"color:#94a3b8;margin-bottom:2px;\">Club</div><div style=\"font-weight:700;color:#0f172a;\">" + escapeHtml(clubName) + "</div></td>"
            + "<td style=\"padding:8px 16px;\"><div style=\"color:#94a3b8;margin-bottom:2px;\">Year</div><div style=\"font-weight:700;color:#0f172a;\">" + escapeHtml(exerciseYear) + "</div></td>"
            + "<td style=\"padding:8px 16px;\"><div style=\"color:#94a3b8;margin-bottom:2px;\">Alerts</div><div style=\"font-weight:700;color:#0f172a;\">" + alertCount + "</div></td>"
            + "<td style=\"padding:8px 0 8px 16px;\"><div style=\"color:#94a3b8;margin-bottom:2px;\">Highest severity</div><div style=\"font-weight:700;color:" + summaryAccent + ";\">" + highestLevel.toUpperCase() + "</div></td>"
            + "</tr></table>"

            // ── Alerts ───────────────────────────────────────────────────────
            + "<h2 style=\"color:#0f172a;font-size:15px;font-weight:700;margin-bottom:16px;padding-bottom:10px;border-bottom:2px solid #f1f5f9;\">Triggered Alerts</h2>"
            + alertsBlock.toString()

            // ── CTA ──────────────────────────────────────────────────────────
            + "<div style=\"text-align:center;margin-top:28px;padding-top:20px;border-top:1px solid #f1f5f9;\">"
            + "<p style=\"font-size:14px;color:#64748b;margin-bottom:0;\">Log in to Cluverse to review transactions and adjust your budgets before limits are exceeded.</p>"
            + "</div>"
            + "</div>"

            // ── Footer ───────────────────────────────────────────────────────
            + "<div style=\"text-align:center;padding:20px;color:#94a3b8;font-size:12px;line-height:1.6;\">"
            + "<p style=\"margin:0;\">This is an automated message from <b>Cluverse</b>. Do not reply to this email.</p>"
            + "<p style=\"margin:4px 0 0 0;opacity:0.7;\">&copy; " + currentYear + " Cluverse &bull; Automated Financial Alerts</p>"
            + "</div>"

            + "</div>"
            + "</body></html>";
    }

    private String getHighestSeverity(List<BudgetAlertItem> alerts) {
        if (alerts == null || alerts.isEmpty()) return "Warning";
        boolean hasLimit = alerts.stream().anyMatch(a -> "limit".equalsIgnoreCase(a.level()));
        if (hasLimit) return "Limit reached";
        boolean hasCritical = alerts.stream().anyMatch(a -> "critical".equalsIgnoreCase(a.level()));
        if (hasCritical) return "Critical";
        return "Warning";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "Warning";
        }
        String normalized = level.trim().toLowerCase();
        if ("limit".equals(normalized)) {
            return "Limit reached";
        }
        if ("critical".equals(normalized)) {
            return "Critical";
        }
        return "Warning";
    }

    private record AlertStyle(String borderColor, String badgeBg, String badgeColor, String barColor, String icon) {}

    private AlertStyle getAlertStyle(String level) {
        if ("Limit reached".equalsIgnoreCase(level)) return new AlertStyle("#be123c", "#ffe4e6", "#be123c", "#be123c", "🔴");
        if ("Critical".equalsIgnoreCase(level))      return new AlertStyle("#dc2626", "#fee2e2", "#dc2626", "#dc2626", "🟠");
        return                                               new AlertStyle("#d97706", "#fef3c7", "#b45309", "#f59e0b", "🟡");
    }
}
