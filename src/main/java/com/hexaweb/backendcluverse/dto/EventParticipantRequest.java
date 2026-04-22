package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.enumerations.ParticipationStatus;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventParticipantRequest {
    private Long eventId;
    private Long userId;          // injecté depuis le token côté service, pas besoin de l'envoyer du frontend
    private Event event; // ou EventDTO

    @Size(max = 500)
    private String comment;

    private String fullName;

    private String email;

    private String phone;

    private String contactInfo;

    @Min(1)
    private Integer reservedSeats;

    private Boolean wantsReminder;

    private ParticipationStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    private String dietaryRequirements;   // régime alimentaire / besoins spéciaux
    private String emergencyContact;      // contact d'urgence
    private String teamName;              // pour hackathons / events en équipe
}