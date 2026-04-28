package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByClubId(Long clubId);
    List<Event> findByCampaignId(Long campaignId);
    @Query("SELECT MONTH(e.startDate), COUNT(e) FROM Event e WHERE e.club.id = :clubId GROUP BY MONTH(e.startDate)")
    List<Object[]> countEventsByMonth(Long clubId);
    List<Event> findByClubIdAndStatusNot(Long clubId, EventStatus status);
    List<Event> findByClubIdAndStatus(Long clubId, EventStatus status);
}