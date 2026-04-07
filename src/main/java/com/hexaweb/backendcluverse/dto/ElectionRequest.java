package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.ElectionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ElectionRequest {
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ElectionStatus status;
    private Long positionId;
    private Long clubId;
}
