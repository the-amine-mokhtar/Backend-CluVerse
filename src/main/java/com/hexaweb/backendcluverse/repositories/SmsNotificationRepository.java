package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.SmsNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SmsNotificationRepository extends JpaRepository<SmsNotification, Long> {
    
    // ✅ Trouver les SMS envoyés pour une participation
    List<SmsNotification> findByParticipationId(Long participationId);
    
    // ✅ Trouver les SMS envoyés pour un event
    List<SmsNotification> findByEventId(Long eventId);
    
    // ✅ Vérifier si un SMS a déjà été envoyé pour une participation
    boolean existsByParticipationIdAndStatus(Long participationId, String status);
}
