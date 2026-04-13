package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.EventCategory;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDTO {

    private Long id;
    private String title;
    private String description;
    private String location;
    private String imageUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer capacity;
    private Integer participantsCount;
    private EventStatus status;
    private EventCategory category;
    private Long clubId;
    private Long campaignId;

    // ── Informations de paiement ──
    private Boolean isPaid;
    private Double price;

    // ── Statut dynamique de capacité ──
    private String capacityStatus; // "AVAILABLE", "FULL", "LIMITED_SEATS"
    private Integer availableSeats;

    // ── Timestamps ──
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
