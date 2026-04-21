package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.enumerations.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {
    List<EventParticipant> findByEventId(Long eventId);
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    List<EventParticipant> findByUserId(Long userId);
    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);
    long countByEventIdAndStatusNot(Long eventId, ParticipationStatus status);
    @Query("SELECT p FROM EventParticipant p JOIN FETCH p.event WHERE p.user.id = :userId")
    List<EventParticipant> findByUserIdWithEvent(Long userId);
    @Query("SELECT COUNT(p) FROM EventParticipant p WHERE p.event.id = :eventId AND p.status != 'CANCELLED'")
    int countByEventIdAndStatusNotCancelled(@Param("eventId") Long eventId);

}

