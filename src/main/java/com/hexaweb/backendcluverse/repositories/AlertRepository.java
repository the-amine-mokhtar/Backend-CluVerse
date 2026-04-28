package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.finance.Alert;
import com.hexaweb.backendcluverse.enumerations.AlertSeverity;
import com.hexaweb.backendcluverse.enumerations.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Optional<Alert> findByStripeTransactionId(String stripeTransactionId);
    List<Alert> findByStatus(AlertStatus status);
    List<Alert> findBySeverity(AlertSeverity severity);
    List<Alert> findByStatusAndSeverity(AlertStatus status, AlertSeverity severity);
    List<Alert> findByClubId(Long clubId);
    List<Alert> findByClubIdAndStatus(Long clubId, AlertStatus status);
    List<Alert> findByClubIdAndSeverity(Long clubId, AlertSeverity severity);
    List<Alert> findByClubIdAndStatusAndSeverity(Long clubId, AlertStatus status, AlertSeverity severity);
    long countByClubIdAndStatus(Long clubId, AlertStatus status);
    long countByClubIdAndStatusAndSeverity(Long clubId, AlertStatus status, AlertSeverity severity);
}
