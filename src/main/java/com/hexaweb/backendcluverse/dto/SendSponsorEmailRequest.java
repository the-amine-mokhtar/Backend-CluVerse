package com.hexaweb.backendcluverse.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendSponsorEmailRequest {
    private String subject;
    private String body;
}
