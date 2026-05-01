package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.MemberPayment;
import com.hexaweb.backendcluverse.entities.finance.Alert;
import com.hexaweb.backendcluverse.entities.finance.Budget;
import com.hexaweb.backendcluverse.entities.finance.Transaction;
import com.hexaweb.backendcluverse.enumerations.AlertSeverity;
import com.hexaweb.backendcluverse.enumerations.AlertStatus;
import com.hexaweb.backendcluverse.enumerations.PaymentStatus;
import com.hexaweb.backendcluverse.enumerations.TransactionType;
import com.hexaweb.backendcluverse.services.Recrutement.GroqService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinanceAgentService {

    private final GroqService groqService;

    public String chat(String userMessage,
                       List<Transaction> transactions,
                       List<Budget> budgets,
                       List<MemberPayment> memberPayments,
                       List<Alert> alerts) {

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                buildSystemPrompt(transactions, budgets, memberPayments, alerts)));
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> response = groqService.chatRaw(messages, 0.3);
        return (String) response.get("content");
    }

    private String buildSystemPrompt(List<Transaction> transactions,
                                     List<Budget> budgets,
                                     List<MemberPayment> payments,
                                     List<Alert> alerts) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are Cluverse Finance Agent, an AI assistant specialized in club financial management.\n");
        sb.append("You have access to real-time financial data for the club. Answer questions accurately based on the data provided.\n");
        sb.append("Be concise, professional, and highlight key concerns first (overdue payments, critical alerts).\n\n");

        // --- Transactions ---
        sb.append("=== TRANSACTIONS ===\n");
        double totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount).sum();
        double totalExpense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount).sum();
        sb.append(String.format("Count: %d | Income: %.2f TND | Expenses: %.2f TND | Net: %.2f TND%n",
                transactions.size(), totalIncome, totalExpense, totalIncome - totalExpense));
        transactions.stream().limit(10).forEach(t ->
                sb.append(String.format("  [%s][%s] %.2f TND on %s: %s%n",
                        t.getType(), t.getScope(), t.getAmount(), t.getDate(), t.getDescription())));
        sb.append("\n");

        // --- Budgets ---
        sb.append("=== BUDGETS ===\n");
        if (budgets.isEmpty()) {
            sb.append("No budgets defined.\n");
        } else {
            budgets.forEach(b -> sb.append(String.format("  Budget #%d [%s] Year %d: %.2f TND allocated%n",
                    b.getId(), b.getBudgetType(), b.getYear(), b.getTotalAllocated())));
        }
        sb.append("\n");

        // --- Member Dues ---
        sb.append("=== MEMBER DUES ===\n");
        long paid = payments.stream().filter(p -> p.getStatus() == PaymentStatus.PAID).count();
        long pending = payments.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).count();
        long overdue = payments.stream().filter(p -> p.getStatus() == PaymentStatus.OVERDUE).count();
        double totalOutstanding = payments.stream()
                .filter(p -> p.getStatus() != PaymentStatus.PAID)
                .mapToDouble(p -> p.getAmount().doubleValue()).sum();
        sb.append(String.format("Total: %d | Paid: %d | Pending: %d | Overdue: %d | Outstanding: %.2f TND%n",
                payments.size(), paid, pending, overdue, totalOutstanding));

        payments.stream().filter(p -> p.getStatus() == PaymentStatus.OVERDUE).forEach(p -> {
            String name = p.getMembership().getUser().getFirstName()
                    + " " + p.getMembership().getUser().getLastName();
            sb.append(String.format("  [OVERDUE] %s: %.2f TND due %s%n", name, p.getAmount(), p.getDueDate()));
        });
        payments.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).forEach(p -> {
            String name = p.getMembership().getUser().getFirstName()
                    + " " + p.getMembership().getUser().getLastName();
            sb.append(String.format("  [PENDING] %s: %.2f TND due %s%n", name, p.getAmount(), p.getDueDate()));
        });
        sb.append("\n");

        // --- Financial Alerts ---
        sb.append("=== FINANCIAL ALERTS ===\n");
        long active = alerts.stream().filter(a -> a.getStatus() == AlertStatus.ACTIVE).count();
        long critical = alerts.stream().filter(a -> a.getStatus() == AlertStatus.ACTIVE && a.getSeverity() == AlertSeverity.CRITICAL).count();
        long high = alerts.stream().filter(a -> a.getStatus() == AlertStatus.ACTIVE && a.getSeverity() == AlertSeverity.HIGH).count();
        long resolved = alerts.stream().filter(a -> a.getStatus() == AlertStatus.RESOLVED).count();
        long dismissed = alerts.stream().filter(a -> a.getStatus() == AlertStatus.DISMISSED).count();
        sb.append(String.format("Total: %d | Active: %d (Critical: %d, High: %d) | Resolved: %d | Dismissed: %d%n",
                alerts.size(), active, critical, high, resolved, dismissed));

        alerts.stream()
                .filter(a -> a.getStatus() == AlertStatus.ACTIVE)
                .sorted((a, b) -> Integer.compare(severityOrder(a.getSeverity()), severityOrder(b.getSeverity())))
                .limit(10)
                .forEach(a -> sb.append(String.format(
                        "  [%s][ACTIVE] %.2f %s | Risk: %d | ML: %.2f | %s | Event: %s%n",
                        a.getSeverity(), a.getAmount(), a.getCurrency(),
                        a.getRiskScore(), a.getMlAnomalyScore(),
                        a.getDescription() != null ? a.getDescription() : "No description",
                        a.getEventType() != null ? a.getEventType() : "N/A")));

        return sb.toString();
    }

    private int severityOrder(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }
}
