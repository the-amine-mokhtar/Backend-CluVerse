package com.hexaweb.backendcluverse.controllers.recrutement;

import com.hexaweb.backendcluverse.entities.recrutement.InterviewConfig;
import com.hexaweb.backendcluverse.services.InterviewConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview-configs")
@RequiredArgsConstructor
public class InterviewConfigController {

    private final InterviewConfigService interviewConfigService;

    @PostMapping("/application/{applicationId}")
    public ResponseEntity<InterviewConfig> create(
            @PathVariable Long applicationId,
            @RequestBody InterviewConfig config) {
        return ResponseEntity.ok(interviewConfigService.createConfig(applicationId, config));
    }

    @GetMapping("/link/{uniqueLink}")
    public ResponseEntity<InterviewConfig> getByLink(@PathVariable String uniqueLink) {
        return ResponseEntity.ok(interviewConfigService.getByUniqueLink(uniqueLink));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<InterviewConfig> getByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewConfigService.getByApplicationId(applicationId));
    }

    @PutMapping("/link/{uniqueLink}/status")
    public ResponseEntity<InterviewConfig> updateStatus(
            @PathVariable String uniqueLink,
            @RequestParam String status) {
        return ResponseEntity.ok(interviewConfigService.updateStatus(uniqueLink, status));
    }

    @GetMapping("/application/{applicationId}/result")
    public ResponseEntity<?> getResult(@PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewConfigService.getInterviewResult(applicationId));
    }

    @GetMapping("/application/{applicationId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewConfigService.getInterviewMessages(applicationId));
    }
}