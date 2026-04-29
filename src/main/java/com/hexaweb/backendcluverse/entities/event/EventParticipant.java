package com.hexaweb.backendcluverse.entities.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.enumerations.ParticipationStatus;
import com.hexaweb.backendcluverse.enumerations.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Contrainte métier :
 *  - Un utilisateur ne peut avoir qu'une seule participation ACTIVE (non CANCELLED)
 *    par événement → vérifié au niveau service (voir EventParticipantService).
 *  - Un utilisateur ne peut pas s'inscrire à deux événements dont les plages horaires
 *    se chevauchent → vérifié au niveau service (voir EventParticipantService#checkNoTimeConflict).
 *
 * L'index composite (event_id, user_id) garantit l'unicité en base.
 */
@Entity
@Table(
    name = "event_participant",
    indexes = {
        @Index(name = "idx_ep_event_status",  columnList = "event_id,status"),
        @Index(name = "idx_ep_user_id",       columnList = "user_id"),
        @Index(name = "idx_ep_status",        columnList = "status"),
        @Index(name = "idx_ep_event_user",    columnList = "event_id,user_id")   // lookup rapide
    },
    uniqueConstraints = {
        // Un seul enregistrement par (user, event) — le service gère CANCELLED vs REGISTERED
        @UniqueConstraint(name = "uq_ep_event_user", columnNames = {"event_id", "user_id"})
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime registrationDate;

    private String fullName;       // ✅ NOUVEAU: stocker le nom complet du participant
    private String participantPhone; // ✅ NOUVEAU: stocker le téléphone du participant
    
    private String comment;
    private String contactInfo;
    private Integer reservedSeats;

    @Builder.Default
    private Boolean wantsReminder = false;

    @Builder.Default
    private Boolean reminderSent = false;

    @Builder.Default
    private Boolean meetingAlertSent = false;

    private String dietaryRequirements;
    private String emergencyContact;
    private String teamName;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ParticipationStatus status = ParticipationStatus.REGISTERED;

    // ── Relations ────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // ── Propriétés JSON calculées ─────────────────────────────────────────

    @Transient
    @JsonProperty("userName")
    public String getUserName() {
        if (this.user == null) return null;
        return String.format("%s %s", this.user.getFirstName(), this.user.getLastName()).trim();
    }

    @Transient
    @JsonProperty("userEmail")
    public String getUserEmail() {
        return this.user == null ? null : this.user.getEmail();
    }

    @Transient
    @JsonProperty("userPhone")
    public String getUserPhone() {
        return this.user == null ? null : this.user.getPhone();
    }

    @Transient
    @JsonProperty("userId")
    public Long getUserId() {
        return this.user == null ? null : this.user.getId();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @PrePersist
    public void prePersist() {
        this.registrationDate = LocalDateTime.now();
        if (this.reservedSeats == null) this.reservedSeats = 1;
        if (this.wantsReminder == null) this.wantsReminder = false;
        if (this.reminderSent  == null) this.reminderSent  = false;
    }
}