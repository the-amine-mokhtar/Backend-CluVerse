package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeechAnalyzerHealthResponse {
    private String status;
    private boolean whisperLoaded;
    private String modelVersion;
}
