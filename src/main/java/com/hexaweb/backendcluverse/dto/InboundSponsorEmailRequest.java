package com.hexaweb.backendcluverse.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InboundSponsorEmailRequest {
    private String subject;
    private String body;
    private String fromAddress;
    private String toAddress;
    private String externalMessageId;
    private String threadId;
    private String inReplyToMessageId;
}
