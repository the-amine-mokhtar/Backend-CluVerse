package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.EventWaitingList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventWaitingListRepository extends JpaRepository<EventWaitingList, Long> {

    Optional<EventWaitingList> findByEventIdAndUserId(Long eventId, Long userId);

    List<EventWaitingList> findByEventIdOrderByJoinedAtAsc(Long eventId);

    List<EventWaitingList> findByUserIdOrderByJoinedAtDesc(Long userId);

    void deleteByEventId(Long eventId);
}