package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponse {
    private String skillName;
    private int targetLevel;
    private List<LearningResource> resources;
    private String estimatedTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningResource {
        private String title;
        private String url;
        private String youtubeId;
        private String searchQuery;
        private String type; // VIDEO, ARTICLE, COURSE
        private String platform;
        private String description;
    }
}
