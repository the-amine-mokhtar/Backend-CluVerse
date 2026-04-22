package com.hexaweb.backendcluverse.entities.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.sponsoring.Sponsorship;
import com.hexaweb.backendcluverse.enumerations.EventCategory;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
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
    @Index(name = "idx_event_club_id", columnList = "club_id"),
    @Index(name = "idx_event_status", columnList = "status")
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
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @Column(name = "participants_count")
    private Integer participantsCount = 0;

    @Column(name = "capacity")
    private Integer capacity = 0;    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.PLANNED;
  @Column(name = "category")
private String category;
    // Event.java


    // Location
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    //  Club (obligatoire)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;
    // Participants
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<EventParticipant> participants = new ArrayList<>();
    // Reservations (autre module)
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Reservation> reservations = new ArrayList<>();
    // Sponsorship
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Sponsorship> sponsorships = new ArrayList<>();
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    //  Campaign (OPTIONNELLE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "events", "campaignAccesses"})
    private Campaign campaign;
    private Boolean isPaid = false;
    private Double price;
    private Boolean reminderSent = false;
    private String currency = "EUR";

    // ── Propriétés transientes exposées en JSON ──
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

    // ── Méthodes utilitaires ──

    /**
     * Obtient le statut de disponibilité de l'événement par rapport à sa capacité
     * @return "AVAILABLE", "LIMITED_SEATS", ou "FULL"
     */
    public String getCapacityStatus() {
        if (capacity == null || capacity <= 0) {
            return "AVAILABLE"; // Capacité illimitée
        }

        if (participantsCount >= capacity) {
            return "FULL";
        }

        int remainingSeats = capacity - participantsCount;
        if (remainingSeats <= (capacity * 0.10)) { // Moins de 10% de places disponibles
            return "LIMITED_SEATS";
        }

        return "AVAILABLE";
    }

    /**
     * Vérifie si l'événement est complet
     */
    public boolean isFull() {
        return capacity != null && capacity > 0 && participantsCount >= capacity;
    }

    /**
     * Obtient le nombre de places disponibles
     */
    public int getAvailableSeats() {
        int count = participantsCount != null ? participantsCount : 0;
        int cap   = capacity         != null ? capacity          : 0;
        if (cap <= 0) return Integer.MAX_VALUE; // illimité
        return Math.max(0, cap - count);
    }
}

