package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteDTO {
    private Long id;
    private LocalDateTime timestamp;
    private boolean isValid;
    private int voteWeight;

    private String voterName;
    private String positionName;
    private CandidateDTO candidate;
    private Long electionId;
    private Long positionId;
}
