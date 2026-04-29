package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.ElectionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ElectionRequest {
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private ElectionStatus status;
    private Long positionId;
    private Long clubId;
}
