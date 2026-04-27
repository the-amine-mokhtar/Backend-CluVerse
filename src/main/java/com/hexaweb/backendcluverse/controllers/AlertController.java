package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.finance.Alert;
import com.hexaweb.backendcluverse.enumerations.AlertSeverity;
import com.hexaweb.backendcluverse.enumerations.AlertStatus;
import com.hexaweb.backendcluverse.services.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<Alert> getAll(@RequestParam(required = false) AlertStatus status,
                               @RequestParam(required = false) AlertSeverity severity) {
        return alertService.findFiltered(status, severity);
    }

    @GetMapping("/{id}")
    public Alert getById(@PathVariable Long id) {
        return alertService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/club/{clubId}")
    public List<Alert> getByClub(@PathVariable Long clubId,
                                  @RequestParam(required = false) AlertStatus status,
                                  @RequestParam(required = false) AlertSeverity severity) {
        if (status != null && severity != null) return alertService.findByClubAndStatusAndSeverity(clubId, status, severity);
        if (status != null) return alertService.findByClubAndStatus(clubId, status);
        if (severity != null) return alertService.findByClubAndSeverity(clubId, severity);
        return alertService.findByClub(clubId);
    }

    @GetMapping("/club/{clubId}/counts")
    public AlertCounts getCounts(@PathVariable Long clubId) {
        return new AlertCounts(
            alertService.countActive(clubId),
            alertService.countBySeverity(clubId, AlertSeverity.CRITICAL),
            alertService.countBySeverity(clubId, AlertSeverity.HIGH),
            alertService.countBySeverity(clubId, AlertSeverity.MEDIUM) + alertService.countBySeverity(clubId, AlertSeverity.LOW)
        );
    }

    // Idempotent: returns existing alert if same stripeTransactionId already saved
    @PostMapping
    public Alert create(@RequestBody Alert alert) {
        if (alert.getStripeTransactionId() != null) {
            Optional<Alert> existing = alertService.findByStripeTransactionId(alert.getStripeTransactionId());
            if (existing.isPresent()) return existing.get();
        }
        return alertService.save(alert);
    }

    @PutMapping("/{id}")
    public Alert update(@PathVariable Long id, @RequestBody Alert alert) {
        alert.setId(id);
        return alertService.save(alert);
    }

    @PatchMapping("/{id}/dismiss")
    public Alert dismiss(@PathVariable Long id) {
        Alert alert = alertService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        alert.setStatus(AlertStatus.DISMISSED);
        return alertService.save(alert);
    }

    @PatchMapping("/{id}/status")
    public Alert updateStatus(@PathVariable Long id, @RequestParam AlertStatus status) {
        Alert alert = alertService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        alert.setStatus(status);
        return alertService.save(alert);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        alertService.deleteById(id);
    }

    public record AlertCounts(long active, long critical, long high, long mediumLow) {}
}
