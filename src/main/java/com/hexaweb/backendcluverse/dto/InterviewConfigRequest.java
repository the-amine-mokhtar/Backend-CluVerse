package com.hexaweb.backendcluverse.dto;

import lombok.Data;

@Data
public class InterviewConfigRequest {
    private Integer duration;
    private String level;
    private String interviewType;
    private String presidentNotes;
}