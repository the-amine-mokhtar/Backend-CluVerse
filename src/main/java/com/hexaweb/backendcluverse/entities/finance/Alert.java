package com.hexaweb.backendcluverse.entities.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.enumerations.AlertSeverity;
import com.hexaweb.backendcluverse.enumerations.AlertStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String stripeTransactionId;

    private String stripeEventId;
    private String stripeStatus;
    private String description;
    private String customer;
    private double amount;
    private String currency;
    private String eventType;
    private int riskScore;
    private double mlAnomalyScore;

    @Column(length = 2000)
    private String reasons;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    @JsonIgnore
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "club_id", nullable = true)
    @JsonIgnore
    private Club club;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
