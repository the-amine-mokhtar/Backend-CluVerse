package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.finance.Alert;
import com.hexaweb.backendcluverse.enumerations.AlertSeverity;
import com.hexaweb.backendcluverse.enumerations.AlertStatus;
import com.hexaweb.backendcluverse.repositories.AlertRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlertService extends EntityServiceImpl<Alert, Long> {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository repository) {
        super(repository);
        this.alertRepository = repository;
    }

    public Optional<Alert> findByStripeTransactionId(String stripeTransactionId) {
        return alertRepository.findByStripeTransactionId(stripeTransactionId);
    }

    public List<Alert> findFiltered(AlertStatus status, AlertSeverity severity) {
        if (status != null && severity != null) return alertRepository.findByStatusAndSeverity(status, severity);
        if (status != null) return alertRepository.findByStatus(status);
        if (severity != null) return alertRepository.findBySeverity(severity);
        return alertRepository.findAll();
    }

    public List<Alert> findByClub(Long clubId) {
        return alertRepository.findByClubId(clubId);
    }

    public List<Alert> findByClubAndStatus(Long clubId, AlertStatus status) {
        return alertRepository.findByClubIdAndStatus(clubId, status);
    }

    public List<Alert> findByClubAndSeverity(Long clubId, AlertSeverity severity) {
        return alertRepository.findByClubIdAndSeverity(clubId, severity);
    }

    public List<Alert> findByClubAndStatusAndSeverity(Long clubId, AlertStatus status, AlertSeverity severity) {
        return alertRepository.findByClubIdAndStatusAndSeverity(clubId, status, severity);
    }

    public long countActive(Long clubId) {
        return alertRepository.countByClubIdAndStatus(clubId, AlertStatus.ACTIVE);
    }

    public long countBySeverity(Long clubId, AlertSeverity severity) {
        return alertRepository.countByClubIdAndStatusAndSeverity(clubId, AlertStatus.ACTIVE, severity);
    }
}
