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
    private Integer termLength;
    private Integer maxCandidates;
    private Boolean electable;
    private Boolean autoRenew;

    private String currentHolderName;
    private Long currentHolderId;
}
