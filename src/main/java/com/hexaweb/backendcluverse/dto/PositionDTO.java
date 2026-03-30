package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionDTO {
    private Long id;
    private String name;
    private String description;
    private int termLength;
    private int maxCandidates;
    private boolean isElectable;
    private boolean isAutoRenew;

    private String currentHolderName;
}
