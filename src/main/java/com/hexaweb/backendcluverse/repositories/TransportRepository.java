package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.logistics.Transport;
import com.hexaweb.backendcluverse.enumerations.TransportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransportRepository extends JpaRepository<Transport, Long> {
	List<Transport> findByVehicle_IdAndStatusAndScheduledDateBetween(
		Long vehicleId,
		TransportStatus status,
		LocalDateTime startDate,
		LocalDateTime endDate
	);

	List<Transport> findByStatusAndScheduledDateBetween(
		TransportStatus status,
		LocalDateTime startDate,
		LocalDateTime endDate
	);
}
