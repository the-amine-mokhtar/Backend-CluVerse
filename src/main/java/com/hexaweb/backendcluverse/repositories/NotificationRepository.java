package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByClubIdOrderByCreatedAtDesc(Long clubId);

    List<Notification> findByClubIdAndReadFalse(Long clubId);

    long countByClubIdAndReadFalse(Long clubId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.clubId = :clubId")
    void markAllAsReadByClubId(@Param("clubId") Long clubId);
}