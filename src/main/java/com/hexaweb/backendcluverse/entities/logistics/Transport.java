package com.hexaweb.backendcluverse.entities.logistics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.enumerations.TransportStatus;
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
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Transport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime scheduledDate;
    private Long departureLocationId;
    private Long arrivalLocationId;
    
    @Column(name = "distance")
    private Double distance;  // Distance in km

    @Column(name = "duration")
    private Double duration;  // Duration in minutes
    
    /**
     * Carburant consommé pour ce transport (en %)
     * Calculé automatiquement: (distance * consumption_rate) / tank_capacity
     */
    @Column(name = "fuel_consumed")
    private Double fuelConsumed = 0.0;  // Percentage 0-100
    
    /**
     * Niveau de carburant après ce transport
     * Utilisé pour afficher l'évolution et déclencher les alertes
     */
    @Column(name = "fuel_level_after")
    private Double fuelLevelAfter;  // Percentage 0-100

    @Enumerated(EnumType.STRING)
    private TransportStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnore
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @JsonIgnore
    private Event event;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Computed getter for vehicleId - extracts from the Vehicle object
     */
    @Transient
    public Long getVehicleId() {
        return vehicle != null ? vehicle.getId() : null;
    }

    /**
     * Computed getter for userId - extracts from the User object
     */
    @Transient
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    /**
     * Computed getter for eventId - extracts from the Event object
     */
    @Transient
    public Long getEventId() {
        return event != null ? event.getId() : null;
    }
}
