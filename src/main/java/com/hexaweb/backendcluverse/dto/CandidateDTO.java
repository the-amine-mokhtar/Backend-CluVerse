package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.CandidateStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDTO {
    private Long id;
    private String program;
    private CandidateStatus status;
    private LocalDateTime submissionDate;
    private LocalDateTime withdrawalDate;
    private String aiCritique;
    private String bio;

    private String userName;
    private String userEmail;
    private Long userId;
    private Long electionId;
    private Long positionId;

    private long voteCount;
    private long totalElectionVotes;
    private double percentage;
}
