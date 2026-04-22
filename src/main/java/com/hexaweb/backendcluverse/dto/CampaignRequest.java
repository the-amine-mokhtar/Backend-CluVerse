package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.CampaignVisibility;
import lombok.Getter;
import lombok.Setter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
public class CampaignRequest {

    private String title;
    private String description;
    private String targetAudience;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")   // ← accepte "2025-06-01T09:00:00" depuis FormData
    private LocalDateTime startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;
    private Integer maxParticipants;
    private Boolean featured;
   private Integer currentParticipants = 0;
    private CampaignVisibility visibility;

    // ⚠️ fichier image upload
    private MultipartFile imageFile;
}