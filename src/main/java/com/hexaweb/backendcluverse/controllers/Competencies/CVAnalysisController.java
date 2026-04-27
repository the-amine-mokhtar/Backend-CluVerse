package com.hexaweb.backendcluverse.controllers.Competencies;

import com.hexaweb.backendcluverse.dto.Competencies.CVAnalysisResponse;
import com.hexaweb.backendcluverse.dto.Competencies.LearningPathResponse;
import com.hexaweb.backendcluverse.services.competencies.CVAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/member-competencies/cv")
@RequiredArgsConstructor
public class CVAnalysisController {

    private final CVAnalysisService cvAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<CVAnalysisResponse> analyzeCV(@RequestParam("userId") Long userId,
                                                        @RequestParam("clubId") Long clubId,
                                                        @RequestParam("file") MultipartFile file) {
        CVAnalysisResponse response = cvAnalysisService.analyzeAndApplyCV(userId, clubId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/learning-path/generate")
    public ResponseEntity<LearningPathResponse> generateLearningPath(@RequestParam("skillName") String skillName,
                                                                   @RequestParam("targetLevel") int targetLevel) {
        LearningPathResponse response = cvAnalysisService.generateLearningPath(skillName, targetLevel);
        return ResponseEntity.ok(response);
    }
}
