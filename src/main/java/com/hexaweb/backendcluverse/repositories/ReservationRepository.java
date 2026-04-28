package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByStartDateBetween(
            LocalDateTime start, LocalDateTime end);
}