package com.hexaweb.backendcluverse.entities.logistics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hexaweb.backendcluverse.enumerations.MaintenanceStatus;
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
public class VehicleMaintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnore
    private Vehicle vehicle;

    private LocalDateTime recordDate;      // when this record was taken
    
    // Vehicle state indicators (0-100 scale)
    private int fuelLevel;           // 0=empty, 100=full
    private int engineCondition;     // 0=critical, 100=perfect
    private int tireCondition;       // 0=critical, 100=perfect
    private int brakeCondition;      // 0=critical, 100=perfect
    private int oilLevel;            // 0=empty, 100=full
    
    // Numeric indicators
    private int mileage;             // total km driven
    private int kmSinceLastService;  // km since last maintenance
    private int daysSinceLastService;// days since last check
    private int totalTransports;     // number of transports done
    
    // Status
    @Enumerated(EnumType.STRING)
    private MaintenanceStatus status; // GOOD, WARNING, CRITICAL
    
    private String notes;
    private boolean resolved;        // true if maintenance was done
}
