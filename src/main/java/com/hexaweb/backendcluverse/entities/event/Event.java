package com.hexaweb.backendcluverse.entities.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.sponsoring.Sponsorship;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import com.hexaweb.backendcluverse.enumerations.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
    @Index(name = "idx_event_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_event_club_id",     columnList = "club_id"),
    @Index(name = "idx_event_status",      columnList = "status"),
    @Index(name = "idx_event_location_dates", columnList = "location_id,start_date,end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    private String title;
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "participants_count")
    private Integer participantsCount = 0;

    @Column(name = "capacity")
    private Integer capacity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.PLANNED;

    @Column(name = "category")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType = EventType.OFFLINE;

    @Column(name = "meeting_url", columnDefinition = "TEXT")
    private String meetingUrl;

    // ── Location (table dédiée) ──────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    // ── Club (obligatoire) ───────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    // ── Relations ────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<EventParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Sponsorship> sponsorships = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "events", "campaignAccesses"})
    private Campaign campaign;

    private Boolean reminderSent = false;

    // ── Propriétés transientes exposées en JSON ──────────────────────────

    @Transient
    @JsonProperty("locationName")
    public String getLocationName() {
        return location != null ? location.getName() : null;
    }

    @Transient
    @JsonProperty("locationAddress")
    public String getLocationAddress() {
        return location != null ? location.getAddress() : null;
    }

    @Transient
    @JsonProperty("locationLatitude")
    public String getLocationLatitude() {
        return location != null ? location.getLatitude() : null;
    }

    @Transient
    @JsonProperty("locationLongitude")
    public String getLocationLongitude() {
        return location != null ? location.getLongitude() : null;
    }

    @Transient
    @JsonProperty("locationId")
    public Long getLocationId() {
        return location != null ? location.getId() : null;
    }

    // ── Méthodes utilitaires ──────────────────────────────────────────────

    public String getCapacityStatus() {
        if (capacity == null || capacity <= 0) return "AVAILABLE";
        if (participantsCount >= capacity)     return "FULL";
        int remaining = capacity - participantsCount;
        return remaining <= (capacity * 0.10) ? "LIMITED_SEATS" : "AVAILABLE";
    }

    public boolean isFull() {
        return capacity != null && capacity > 0 && participantsCount >= capacity;
    }

    public int getAvailableSeats() {
        int count = participantsCount != null ? participantsCount : 0;
        int cap   = capacity         != null ? capacity          : 0;
        if (cap <= 0) return Integer.MAX_VALUE;
        return Math.max(0, cap - count);
    }

    public boolean isOnline() {
        return eventType == EventType.ONLINE;
    }

    /**
     * Vérifie si cet événement chevauche temporellement un autre
     * (utilisé par EventService pour la validation de conflit de terrain).
     */
    public boolean overlapsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        if (this.startDate == null || this.endDate == null) return false;
        // Chevauchement : startA < endB && endA > startB
        return this.startDate.isBefore(otherEnd) && this.endDate.isAfter(otherStart);
    }
}
