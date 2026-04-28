package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.ReservationStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReservationRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ReservationStatus status;
    private int quantityReserved;
    private String notes;
    private Long eventId;
    private Long resourceId;
    private Long userId;
}
