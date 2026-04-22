package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.logistics.VehicleMaintenance;
import com.hexaweb.backendcluverse.enumerations.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, Long> {
    List<VehicleMaintenance> findByVehicleIdOrderByRecordDateDesc(Long vehicleId);
    List<VehicleMaintenance> findByStatusOrderByRecordDateDesc(MaintenanceStatus status);
    Optional<VehicleMaintenance> findTopByVehicleIdOrderByRecordDateDesc(Long vehicleId);
    List<VehicleMaintenance> findByResolvedFalse();
}
