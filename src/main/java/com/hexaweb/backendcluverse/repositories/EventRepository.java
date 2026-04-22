package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    // ✅ Charger un événement avec sa location pour éviter les erreurs N+1
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.location WHERE e.id = :id")
    Optional<Event> findByIdWithLocation(@Param("id") Long id);

    @Query("SELECT COUNT(p) FROM EventParticipant p WHERE p.event.campaign.id = :campaignId")
    Integer countParticipantsByCampaignId(@Param("campaignId") Long campaignId);
    List<Event> findByClubId(Long clubId);

    List<Event> findByCampaignId(Long campaignId);

    // ✅ Trouver les events entre deux dates (pour le scheduler SMS)
    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :from AND :to")
    List<Event> findByStartDateBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // ✅ Ancienne version (garder pour compatibilité)
@Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :from AND :to")
List<Event> findEventsStartingBetween(LocalDateTime from, LocalDateTime to);
    @Query("SELECT MONTH(e.startDate), COUNT(e) FROM Event e WHERE e.club.id = :clubId GROUP BY MONTH(e.startDate)")
    List<Object[]> countEventsByMonth(Long clubId);
}