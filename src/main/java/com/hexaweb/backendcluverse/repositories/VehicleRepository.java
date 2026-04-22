package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByIsAvailableTrue();
    
    /**
     * Find vehicle by model and plate number.
     * Used for validating unique constraint: matricule unique per type.
     */
    @Query("SELECT v FROM Vehicle v WHERE v.model = :model AND v.plateNumber = :plateNumber")
    Optional<Vehicle> findByModelAndPlateNumber(
        @Param("model") String model,
        @Param("plateNumber") String plateNumber
    );
}
