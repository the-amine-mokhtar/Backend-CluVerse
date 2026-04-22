package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteTestResponse {
    private String status;
    private int votesCreated;
    private int votesDeleted;
    private String message;
}
