package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SponsorEmailAttachmentDto {
    private Long id;
    private String originalFileName;
    private String contentType;
    private Long sizeBytes;
    private String fileUrl;
}
