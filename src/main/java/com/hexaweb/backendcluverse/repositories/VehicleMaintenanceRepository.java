package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.logistics.VehicleMaintenance;
import com.hexaweb.backendcluverse.enumerations.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, Long> {
    List<VehicleMaintenance> findByVehicleIdOrderByRecordDateDesc(Long vehicleId);
    
    @Query("SELECT m FROM VehicleMaintenance m LEFT JOIN FETCH m.vehicle WHERE m.status = :status ORDER BY m.recordDate DESC")
    List<VehicleMaintenance> findByStatusOrderByRecordDateDesc(MaintenanceStatus status);
    
    Optional<VehicleMaintenance> findTopByVehicleIdOrderByRecordDateDesc(Long vehicleId);
    List<VehicleMaintenance> findByResolvedFalse();
}
