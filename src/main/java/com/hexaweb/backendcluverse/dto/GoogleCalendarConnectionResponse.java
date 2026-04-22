package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarConnectionResponse {
    private boolean connected;
    private String message;
}
