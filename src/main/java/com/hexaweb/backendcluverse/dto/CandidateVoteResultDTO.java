package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateVoteResultDTO {
    private Long candidateId;
    private String firstName;
    private String lastName;
    private String fullName;
    private long votes;
}
