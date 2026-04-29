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
    @Query("SELECT COUNT(e) FROM Event e WHERE e.campaign.id = :campaignId")
    long countByCampaignId(@Param("campaignId") Long campaignId);
    // ✅ Trouver les events entre deux dates (pour le scheduler SMS)
    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :from AND :to")
    List<Event> findByStartDateBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

   @Query("SELECT MONTH(e.startDate), COUNT(e) FROM Event e WHERE e.club.id = :clubId GROUP BY MONTH(e.startDate)")
    List<Object[]> countEventsByMonth(Long clubId);

    // Cette méthode doit exister dans EventRepository.java
    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :from AND :to")
    List<Event> findEventsStartingBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

      @Query("""
        SELECT e FROM Event e
        WHERE e.location.id = :locationId
          AND e.status <> 'CANCELLED'
          AND (:excludeId IS NULL OR e.id <> :excludeId)
          AND e.startDate < :endDate
          AND e.endDate   > :startDate
    """)
    List<Event> findConflictingEventsOnLocation(
            @Param("locationId") Long locationId,
            @Param("startDate")  LocalDateTime startDate,
            @Param("endDate")    LocalDateTime endDate,
            @Param("excludeId")  Long excludeId   // null pour création, id pour update
    );
    @Query("""
        SELECT e FROM Event e
        JOIN e.participants p
        WHERE p.user.id = :userId
          AND p.status  <> 'CANCELLED'
          AND e.status  <> 'CANCELLED'
          AND (:excludeEventId IS NULL OR e.id <> :excludeEventId)
          AND e.startDate < :endDate
          AND e.endDate   > :startDate
    """)
    List<Event> findOverlappingEventsForUser(
            @Param("userId")         Long userId,
            @Param("startDate")      LocalDateTime startDate,
            @Param("endDate")        LocalDateTime endDate,
            @Param("excludeEventId") Long excludeEventId  // l'event en cours (pour réactivation)
    );
    
    // ✅ Query pour chercher les événements ONLINE commençant dans ~1 heure
    @Query("""
        SELECT e FROM Event e
        WHERE e.eventType = 'ONLINE'
          AND e.meetingUrl IS NOT NULL
          AND e.status <> 'CANCELLED'
          AND e.startDate BETWEEN :from AND :to
        ORDER BY e.startDate ASC
    """)
    List<Event> findOnlineEventsStartingBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );
   }