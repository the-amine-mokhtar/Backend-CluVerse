package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.SponsorEmailDirection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SponsorEmailDto {
    private Long id;
    private String subject;
    private String body;
    private LocalDateTime sentAt;
    private SponsorEmailDirection direction;
    private Long inReplyToId;
    private Boolean pinned;
    private String fromAddress;
    private String toAddress;
    private String externalMessageId;
    private String threadId;
    private String inReplyToMessageId;
    private List<SponsorEmailAttachmentDto> attachments;
}
