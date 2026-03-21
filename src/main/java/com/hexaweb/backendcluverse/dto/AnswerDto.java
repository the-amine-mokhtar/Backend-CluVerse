package com.hexaweb.backendcluverse.dto;

import lombok.Data;

@Data
public class AnswerDto {
    private Long questionId;
    private String answer;
}