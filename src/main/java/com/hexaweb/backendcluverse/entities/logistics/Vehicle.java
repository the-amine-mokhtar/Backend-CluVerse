package com.hexaweb.backendcluverse.entities.logistics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_vehicle_model_plate",
        columnNames = {"model", "plateNumber"}
    )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("plateNumber")
    private String plateNumber;

    @JsonProperty("model")
    private String model;

    @JsonProperty("available")
    private boolean isAvailable;
    
    /**
     * Kilométrage total cumulé (somme de tous les transports effectués)
     * Augmente automatiquement lors de chaque création de transport
     * Utilisé pour: "La distance augmente lorsque je choisie la voiture pour plusieurs transport"
     */
    @JsonProperty("totalKilometers")
    @Column(name = "total_kilometers", nullable = false, columnDefinition = "DOUBLE DEFAULT 0.0")
    private Double totalKilometers = 0.0;
    
    /**
     * Niveau de carburant du véhicule (0-100 %)
     * Commence à 100 (plein) et diminue avec chaque transport
     * Utilisé pour: Alerte de carburant et planification des stations-service
     */
    @JsonProperty("fuelLevel")
    @Column(name = "fuel_level", nullable = false, columnDefinition = "DOUBLE DEFAULT 100.0")
    private Double fuelLevel = 100.0;
    
    /**
     * Capacité du réservoir en litres (approx pour cette marque/modèle)
     * Utilisé pour calculer la consommation réelle
     */
    @JsonProperty("fuelTankCapacity")
    @Column(name = "fuel_tank_capacity", nullable = false, columnDefinition = "DOUBLE DEFAULT 60.0")
    private Double fuelTankCapacity = 60.0; // litres par défaut

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Transport> transports = new ArrayList<>();
}
