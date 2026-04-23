package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.enumerations.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {


    @Query("""
        SELECT p FROM EventParticipant p
        JOIN FETCH p.user u
        WHERE p.user.id = :userId
    """)
    List<EventParticipant> findByUserId(@Param("userId") Long userId);


    List<EventParticipant> findByEventIdAndStatusNot(Long eventId, ParticipationStatus status);

    @Query("SELECT ep FROM EventParticipant ep WHERE ep.event.campaign.id = :campaignId")
    List<EventParticipant> findByCampaignId(Long campaignId);

    @Query("""
        SELECT COUNT(p) FROM EventParticipant p
        JOIN p.event e
        WHERE e.campaign.id = :campaignId
        AND p.status <> 'CANCELLED'
    """)
    long countParticipantsByCampaign(@Param("campaignId") Long campaignId);




    @Query("""
        SELECT COUNT(p)
        FROM EventParticipant p
        WHERE p.event.campaign.id = :campaignId
        AND p.status <> com.hexaweb.backendcluverse.enumerations.ParticipationStatus.CANCELLED
    """)
    Long countParticipantsByCampaignId(@Param("campaignId") Long campaignId);

    @Query("""
        SELECT COUNT(p)
        FROM EventParticipant p
        WHERE p.event.campaign.id = :campaignId
    """)
    Long countByCampaignId(@Param("campaignId") Long campaignId);


    @Query("SELECT p FROM EventParticipant p " +
            "JOIN FETCH p.user u " +
            "WHERE p.event.id = :eventId " +
            "AND p.status = :status " +
            "AND p.wantsReminder = true " +
            "AND p.reminderSent = false")
    List<EventParticipant> findParticipantsForReminder(
            @Param("eventId") Long eventId,
            @Param("status") ParticipationStatus status
    );

    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);
    List<EventParticipant> findByEventId(Long eventId);
    long countByEventIdAndStatusNot(Long eventId, ParticipationStatus status);

    @Query("SELECT p FROM EventParticipant p JOIN FETCH p.event WHERE p.user.id = :userId")
    List<EventParticipant> findByUserIdWithEvent(@Param("userId") Long userId);

    void deleteByEventId(Long eventId);

}