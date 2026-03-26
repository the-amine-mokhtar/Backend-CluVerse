package com.hexaweb.backendcluverse.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApplicationSubmissionDto {
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private List<AnswerDto> answers;
}