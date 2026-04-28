package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.MemberPayment;
import com.hexaweb.backendcluverse.entities.finance.Alert;
import com.hexaweb.backendcluverse.entities.finance.Budget;
import com.hexaweb.backendcluverse.entities.finance.Transaction;
import com.hexaweb.backendcluverse.repositories.BudgetRepository;
import com.hexaweb.backendcluverse.repositories.TransactionRepository;
import com.hexaweb.backendcluverse.services.AlertService;
import com.hexaweb.backendcluverse.services.FinanceAgentService;
import com.hexaweb.backendcluverse.services.MemberPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance-agent")
@RequiredArgsConstructor
public class FinanceAgentController {

    private final FinanceAgentService financeAgentService;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final MemberPaymentService memberPaymentService;
    private final AlertService alertService;

    @PostMapping("/chat/{clubId}")
    public ResponseEntity<Map<String, String>> chat(
            @PathVariable Long clubId,
            @RequestBody Map<String, String> request) {

        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
        }

        List<Transaction> transactions = transactionRepository.findByClubId(clubId);
        List<Budget> budgets = budgetRepository.findByClubId(clubId);
        List<MemberPayment> payments = memberPaymentService.findByClub(clubId);
        List<Alert> alerts = alertService.findByClub(clubId);

        String response = financeAgentService.chat(message, transactions, budgets, payments, alerts);
        return ResponseEntity.ok(Map.of("response", response));
    }
}
