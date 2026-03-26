package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
