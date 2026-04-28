package com.hexaweb.backendcluverse.entities.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.sponsoring.Sponsorship;
import com.hexaweb.backendcluverse.enumerations.EventCategory;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
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
    private String location;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer capacity;
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    @Enumerated(EnumType.STRING)
    private EventCategory category;
    // Event.java
    private Integer participantsCount = 0;

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
    @JoinColumn(name = "campaign_id", nullable = true)
    private Campaign campaign;
    private Boolean isPaid = false;
    private Double price;

    // Coordonnées GPS pour la carte
    private Double latitude;
    private Double longitude;

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
    public Integer getAvailableSeats() {
        if (capacity == null || capacity <= 0) {
            return -1; // Capacité illimitée
        }
        return Math.max(0, capacity - participantsCount);
    }
}

