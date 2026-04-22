package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("select r from Reservation r join fetch r.resource where r.event.id = :eventId")
    List<Reservation> findByEventId(@Param("eventId") Long eventId);
}
