package com.hexaweb.backendcluverse.entities.event;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ✅ Entity pour tracker les notifications SMS envoyées
 */
@Entity
@Table(name = "sms_notifications")
public class SmsNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long participationId;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column
    private String smsProviderReference; // ID du fournisseur SMS

    @Column
    private String status; // SENT, FAILED, PENDING

    @Column
    private String errorMessage;

    // ── Getters & Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getParticipationId() { return participationId; }
    public void setParticipationId(Long participationId) { this.participationId = participationId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public String getSmsProviderReference() { return smsProviderReference; }
    public void setSmsProviderReference(String smsProviderReference) { this.smsProviderReference = smsProviderReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
