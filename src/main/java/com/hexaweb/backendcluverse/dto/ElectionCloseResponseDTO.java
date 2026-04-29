package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ElectionCloseResponseDTO {
    private Long electionId;
    private String electionTitle;
    private String positionName;
    private String clubName;
    private LocalDate startDate;
    private LocalDate closedAt;
    private Long winnerCandidateId;
    private String winnerFirstName;
    private String winnerLastName;
    private List<CandidateVoteResultDTO> candidates;
}
