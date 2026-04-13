package com.hexaweb.backendcluverse.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PositionRequest {
    private String name;
    private String description;
    private Integer termLength;
    private Integer maxCandidates;
    private Boolean electable;
    private Boolean autoRenew;
    private Long clubId;
    private Long currentHolderId;
}
