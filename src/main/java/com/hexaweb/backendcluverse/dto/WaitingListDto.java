package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.entities.event.EventWaitingList;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour représenter la file d'attente comme une participation
 * pour que le frontend puisse l'afficher dans la même liste
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingListDto {
    private Long id;
    private String status;  // "WAITING_LIST"
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventStartDate;
    private String eventImageUrl;
    private LocalDateTime joinedAt;
    private Integer positionInQueue;  // Position dans la file (1-based)

    /**
     * Convertir une EventWaitingList en DTO
     */
    public static WaitingListDto fromEntity(EventWaitingList wl, Integer position) {
        Event event = wl.getEvent();
        return WaitingListDto.builder()
                .id(wl.getId())
                .status("WAITING_LIST")
                .eventId(event != null ? event.getId() : null)
                .eventTitle(event != null ? event.getTitle() : "Unknown")
                .eventStartDate(event != null ? event.getStartDate() : null)
                .eventImageUrl(event != null ? event.getImageUrl() : null)
                .joinedAt(wl.getJoinedAt())
                .positionInQueue(position)
                .build();
    }
}
