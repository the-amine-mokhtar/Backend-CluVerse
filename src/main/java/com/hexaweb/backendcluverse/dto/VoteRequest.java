package com.hexaweb.backendcluverse.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoteRequest {
    private Long candidateId;
    private Long electionId;
}
