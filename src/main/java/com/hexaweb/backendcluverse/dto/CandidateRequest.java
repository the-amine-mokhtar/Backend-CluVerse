package com.hexaweb.backendcluverse.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateRequest {
    private Long positionId;
    private Long electionId;
    private String program;
    private String bio;
}
