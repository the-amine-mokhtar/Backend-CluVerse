package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Notification;
import com.hexaweb.backendcluverse.repositories.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(@RequestParam Long clubId) {
        return ResponseEntity.ok(notificationRepository.findByClubIdOrderByCreatedAtDesc(clubId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok("Notification marked as read");
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestParam Long clubId) {
        notificationRepository.markAllAsReadByClubId(clubId);
        return ResponseEntity.ok("All notifications marked as read");
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestParam Long clubId) {
        return ResponseEntity.ok(notificationRepository.countByClubIdAndReadFalse(clubId));
    }

    @PostMapping("/budget-alert-email")
    public ResponseEntity<String> sendBudgetAlertEmail(@RequestBody BudgetAlertEmailRequest request) {
        try {
            String to = (request.recipientEmail() != null && !request.recipientEmail().isBlank())
                    ? request.recipientEmail() : fromAddress;

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);

            String highestLevel = highestLevel(request.alerts());
            String levelLabel = levelLabel(highestLevel);
            helper.setSubject("🚨 " + levelLabel + " — " + request.alerts().size() + " budget alerts for " + request.clubName());
            helper.setText(buildAlertHtml(request, highestLevel, levelLabel), true);
            mailSender.send(mime);

            return ResponseEntity.ok("Budget alert email sent");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send email: " + e.getMessage());
        }
    }

    private String highestLevel(List<BudgetAlertItem> alerts) {
        boolean hasLimit = alerts.stream().anyMatch(a -> "limit".equals(a.level()));
        if (hasLimit) return "limit";
        boolean hasCritical = alerts.stream().anyMatch(a -> "critical".equals(a.level()));
        if (hasCritical) return "critical";
        return "warning";
    }

    private String levelLabel(String level) {
        return switch (level) {
            case "limit" -> "LIMIT REACHED";
            case "critical" -> "CRITICAL";
            default -> "WARNING";
        };
    }

    private String levelColor(String level) {
        return switch (level) {
            case "limit" -> "#ef4444";
            case "critical" -> "#f97316";
            default -> "#f59e0b";
        };
    }

    private String buildAlertHtml(BudgetAlertEmailRequest req, String highestLevel, String highestLabel) {
        String bg = "#0a0a1a";
        String card = "#131329";
        String border = "rgba(255,255,255,0.08)";
        String muted = "#64748b";
        String sub = "#94a3b8";
        String white = "#f8fafc";
        String text = "#e2e8f0";
        String highColor = levelColor(highestLevel);

        String name = req.recipientName() != null ? req.recipientName() : "Treasurer";
        int year = req.exerciseYear() != null ? req.exerciseYear() : java.time.Year.now().getValue();
        int count = req.alerts().size();

        // Summary table row
        String summaryRow =
            "<tr>" +
            "<td style=\"padding:10px 14px;font-size:13px;color:" + text + ";\">" + esc(req.clubName()) + "</td>" +
            "<td style=\"padding:10px 14px;font-size:13px;color:" + text + ";\">" + year + "</td>" +
            "<td style=\"padding:10px 14px;font-size:13px;color:" + text + ";\">" + count + "</td>" +
            "<td style=\"padding:10px 14px;\">" +
            "<span style=\"background:" + highColor + ";color:#fff;padding:3px 10px;border-radius:20px;font-size:11px;font-weight:600;\">" + highestLabel + "</span>" +
            "</td></tr>";

        // Alert cards
        StringBuilder alertCards = new StringBuilder();
        for (BudgetAlertItem alert : req.alerts()) {
            String color = levelColor(alert.level());
            String label = levelLabel(alert.level());
            long barPct = Math.min(Math.round(alert.utilization()), 100);
            alertCards.append(
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:12px;\">" +
                "<tr>" +
                "<td style=\"width:3px;background:" + color + ";border-radius:3px 0 0 3px;\"></td>" +
                "<td style=\"background:" + card + ";border:1px solid " + border + ";border-left:none;border-radius:0 8px 8px 0;padding:16px;\">" +

                // Top row: department + badge
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>" +
                "<td style=\"font-size:12px;color:" + muted + ";\"><span style=\"color:" + color + ";\">&#9679;</span> " + esc(alert.department()) + "</td>" +
                "<td align=\"right\"><span style=\"background:" + color + ";color:#fff;padding:3px 10px;border-radius:20px;font-size:11px;font-weight:600;\">" + label + "</span></td>" +
                "</tr></table>" +

                // Title
                "<p style=\"margin:10px 0 12px;font-size:15px;font-weight:700;color:" + white + ";\">" + esc(alert.title()) + "</p>" +

                // Budget consumed row
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>" +
                "<td style=\"font-size:12px;color:" + sub + ";\">Budget consumed</td>" +
                "<td align=\"right\" style=\"font-size:12px;color:" + color + ";font-weight:700;\">" +
                String.format(java.util.Locale.US, "%.0f%%", alert.utilization()) + "</td>" +
                "</tr></table>" +

                // Progress bar
                "<div style=\"height:6px;background:rgba(255,255,255,0.08);border-radius:3px;margin:8px 0;\">" +
                "<div style=\"height:6px;width:" + barPct + "%;background:" + color + ";border-radius:3px;\"></div>" +
                "</div>" +

                // Threshold
                "<p style=\"margin:0;font-size:12px;color:" + muted + ";\">Alert threshold: " +
                String.format(java.util.Locale.US, "%.0f%%", alert.reachedThreshold()) + "</p>" +

                "</td></tr></table>"
            );
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>" +
            "<body style=\"margin:0;padding:0;background:" + bg + ";font-family:'Segoe UI',Arial,sans-serif;color:" + text + ";\">" +
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + bg + ";padding:40px 20px;\">" +
            "<tr><td align=\"center\">" +
            "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;\">" +

            // Label + title + date
            "<tr><td style=\"padding-bottom:4px;\">" +
            "<p style=\"margin:0 0 6px;font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#475569;\">CLUVERSE FINANCE</p>" +
            "<h1 style=\"margin:0 0 4px;font-size:26px;font-weight:700;color:" + white + ";\">Budget Alert</h1>" +
            "<p style=\"margin:0;font-size:13px;color:" + muted + ";\">" + year + " &bull; Detected " + esc(req.triggeredAt()) + "</p>" +
            "</td></tr>" +

            // Separator
            "<tr><td style=\"padding:16px 0 20px;\"><div style=\"height:1px;background:" + border + ";\"></div></td></tr>" +

            // Greeting
            "<tr><td style=\"padding-bottom:20px;\">" +
            "<p style=\"margin:0 0 10px;font-size:15px;\">Hello <strong>" + esc(name) + "</strong>,</p>" +
            "<p style=\"margin:0;color:" + sub + ";font-size:14px;line-height:1.7;\">" +
            "<strong style=\"color:" + text + ";\">" + count + " budget alert" + (count > 1 ? "s" : "") + "</strong>" +
            " have been triggered for <strong style=\"color:" + text + ";\">" + esc(req.clubName()) + "</strong>. " +
            "Please review the details below and take corrective action if needed to maintain financial stability." +
            "</p>" +
            "</td></tr>" +

            // Summary table
            "<tr><td style=\"padding-bottom:24px;\">" +
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + card + ";border:1px solid " + border + ";border-radius:8px;\">" +
            "<tr style=\"border-bottom:1px solid " + border + ";\">" +
            "<th style=\"padding:10px 14px;text-align:left;font-size:11px;letter-spacing:1px;text-transform:uppercase;color:" + muted + ";font-weight:500;border-bottom:1px solid " + border + ";\">Club</th>" +
            "<th style=\"padding:10px 14px;text-align:left;font-size:11px;letter-spacing:1px;text-transform:uppercase;color:" + muted + ";font-weight:500;border-bottom:1px solid " + border + ";\">Year</th>" +
            "<th style=\"padding:10px 14px;text-align:left;font-size:11px;letter-spacing:1px;text-transform:uppercase;color:" + muted + ";font-weight:500;border-bottom:1px solid " + border + ";\">Alerts</th>" +
            "<th style=\"padding:10px 14px;text-align:left;font-size:11px;letter-spacing:1px;text-transform:uppercase;color:" + muted + ";font-weight:500;border-bottom:1px solid " + border + ";\">Highest severity</th>" +
            "</tr>" +
            summaryRow +
            "</table>" +
            "</td></tr>" +

            // Triggered alerts
            "<tr><td style=\"padding-bottom:16px;\">" +
            "<h3 style=\"margin:0;font-size:15px;font-weight:600;color:" + white + ";\">Triggered Alerts</h3>" +
            "</td></tr>" +
            "<tr><td style=\"padding-bottom:24px;\">" + alertCards + "</td></tr>" +

            // Footer
            "<tr><td style=\"border-top:1px solid " + border + ";padding-top:20px;\">" +
            "<p style=\"margin:0 0 12px;color:" + muted + ";font-size:13px;line-height:1.7;\">Log in to Cluverse to review transactions and adjust your budgets before limits are exceeded.</p>" +
            "<p style=\"margin:0 0 8px;color:#475569;font-size:12px;\">This is an automated message from Cluverse. Do not reply.</p>" +
            "<p style=\"margin:0;color:#334155;font-size:11px;\">&copy; " + year + " Cluverse &bull; Automated Financial Alerts</p>" +
            "</td></tr>" +

            "</table></td></tr></table></body></html>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public record BudgetAlertItem(
            String title,
            String department,
            double utilization,
            double reachedThreshold,
            String level
    ) {}

    public record BudgetAlertEmailRequest(
            String recipientEmail,
            String recipientName,
            String clubName,
            Integer exerciseYear,
            List<BudgetAlertItem> alerts,
            String triggeredAt
    ) {}
}
