package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.TransportStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TransportRequest {
    private LocalDateTime scheduledDate;
    private Long departureLocationId;
    private Long arrivalLocationId;
    private Double distance;  // Distance in km
    private TransportStatus status;
    private Long vehicleId;
    private Long userId;
    private Long eventId;
}
