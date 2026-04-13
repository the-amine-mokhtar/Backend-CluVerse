package com.hexaweb.backendcluverse.entities.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.enumerations.ParticipationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime registrationDate;

    private String comment;
    private String contactInfo;
    private Integer reservedSeats;
    private Boolean wantsReminder = false;

    private String dietaryRequirements;
    private String emergencyContact;
    private String teamName;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    @Enumerated(EnumType.STRING)
    private ParticipationStatus status = ParticipationStatus.REGISTERED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @JsonProperty("userName")
    public String getUserName() {
        if (this.user == null) return null;
        return String.format("%s %s", this.user.getFirstName(), this.user.getLastName()).trim();
    }

    @JsonProperty("userEmail")
    public String getUserEmail() {
        return this.user == null ? null : this.user.getEmail();
    }

    @JsonProperty("userPhone")
    public String getUserPhone() {
        return this.user == null ? null : this.user.getPhone();
    }

    // ✅ Paiement
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String paymentMethod;
    private BigDecimal totalAmount;
    private LocalDateTime paymentDate;

    @PrePersist
    public void prePersist() {
        this.registrationDate = LocalDateTime.now();
        if (this.reservedSeats == null) this.reservedSeats = 1;
        if (this.wantsReminder == null) this.wantsReminder = false;
    }
}