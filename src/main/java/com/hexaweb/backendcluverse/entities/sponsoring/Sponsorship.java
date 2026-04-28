package com.hexaweb.backendcluverse.entities.sponsoring;

import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.enumerations.SponsorshipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sponsorship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double amount;
    private LocalDate startDate;
    private LocalDate endDate;

    private String eventName;
    private String ownerName;

    @Column(length = 4000)
    private String proposalSummary;

    private String proposalDocumentName;
    private String contractDocumentName;
    private String signedDocumentName;
    private String contractReference;

    private BigDecimal expectedAmount;
    private BigDecimal agreedAmount;
    private BigDecimal paidAmount;

    private LocalDateTime outreachSentAt;
    private String outreachResponseToken;
    private String outreachDecision;
    private LocalDateTime outreachRespondedAt;
    private String signedUploadToken;
    private String paymentPageToken;
    private LocalDateTime proposalSentAt;
    private LocalDateTime contractSentAt;
    private LocalDateTime signedAt;
    private LocalDateTime paidAt;

    @Column(length = 4000)
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private SponsorshipStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sponsor_id", nullable = false)
    private Sponsor sponsor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = SponsorshipStatus.PROSPECTING;
        }
        if (outreachDecision == null) {
            outreachDecision = "PENDING";
        }
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
    }
}

