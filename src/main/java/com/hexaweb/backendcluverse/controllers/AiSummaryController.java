package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.GroqSummaryRequest;
import com.hexaweb.backendcluverse.dto.GroqSummaryResponse;
import com.hexaweb.backendcluverse.dto.SponsorshipProposalSummaryRequest;
import com.hexaweb.backendcluverse.services.GroqSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiSummaryController {

    private final GroqSummaryService groqSummaryService;

    @GetMapping("/summary/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "provider", "groq",
                "configured", groqSummaryService.isConfigured()
        ));
    }

    @PostMapping("/summary")
    public ResponseEntity<GroqSummaryResponse> summarize(@RequestBody GroqSummaryRequest request) {
        try {
            return ResponseEntity.ok(groqSummaryService.summarize(request));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/summary/sponsorship-proposal")
    public ResponseEntity<GroqSummaryResponse> summarizeSponsorshipProposal(
            @RequestBody SponsorshipProposalSummaryRequest request
    ) {
        try {
            return ResponseEntity.ok(groqSummaryService.generateSponsorshipProposalSummary(request));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
