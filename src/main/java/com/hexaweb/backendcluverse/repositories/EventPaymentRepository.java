package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.EventPayment;
import com.hexaweb.backendcluverse.enumerations.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventPaymentRepository extends JpaRepository<EventPayment, Long> {

    List<EventPayment> findByEventParticipantId(Long eventParticipantId);

    List<EventPayment> findByEventId(Long eventId);

    List<EventPayment> findByEventIdAndStatus(Long eventId, PaymentStatus status);

    Optional<EventPayment> findByTransactionId(String transactionId);

    @Query("SELECT SUM(ep.amount) FROM EventPayment ep WHERE ep.event.id = :eventId AND ep.status = 'COMPLETED'")
    Optional<BigDecimal> getTotalRevenue(Long eventId);

    @Query("SELECT ep FROM EventPayment ep WHERE ep.event.id = :eventId AND ep.paidAt >= :startDate AND ep.paidAt <= :endDate")
    List<EventPayment> findPaymentsByEventAndDateRange(Long eventId, LocalDateTime startDate, LocalDateTime endDate);
}
