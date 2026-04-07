package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
