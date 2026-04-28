package com.hexaweb.backendcluverse.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.hexaweb.backendcluverse.entities.finance.Transaction;
import com.hexaweb.backendcluverse.enumerations.TransactionType;
import com.hexaweb.backendcluverse.repositories.TransactionRepository;
import com.hexaweb.backendcluverse.services.TransactionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public List<Transaction> getAll() {
        return transactionService.findAll();
    }

    @GetMapping("/{id}")
    public Transaction getById(@PathVariable Long id) {
        return transactionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/sponsor/{sponsorId}")
    public List<Transaction> getBySponsor(@PathVariable Long sponsorId) {
        return transactionRepository.findBySponsorId(sponsorId);
    }

    @GetMapping("/sponsor/{sponsorId}/donations")
    public List<Transaction> getSponsorDonations(@PathVariable Long sponsorId) {
        return transactionRepository.findBySponsorIdAndType(sponsorId, TransactionType.INCOME);
    }

    @GetMapping("/sponsor/{sponsorId}/range")
    public List<Transaction> getBySponsorAndRange(
            @PathVariable Long sponsorId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) TransactionType type
    ) {
        if (type == null) {
            return transactionRepository.findBySponsorIdAndDateBetween(sponsorId, startDate, endDate);
        }

        return transactionRepository.findBySponsorIdAndTypeAndDateBetween(sponsorId, type, startDate, endDate);
    }

    @PostMapping
    public Transaction create(@RequestBody Transaction transaction) {
        return transactionService.save(transaction);
    }

    @PutMapping("/{id}")
    public Transaction update(@PathVariable Long id, @RequestBody Transaction transaction) {
        transaction.setId(id);
        return transactionService.save(transaction);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        transactionService.deleteById(id);
    }
}

