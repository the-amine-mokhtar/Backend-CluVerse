package com.hexaweb.backendcluverse.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PositionRequest {
    private String name;
    private String description;
    private int termLength;
    private int maxCandidates;
    private boolean isElectable;
    private boolean isAutoRenew;
    private Long clubId;
    private Long currentHolderId;
}
