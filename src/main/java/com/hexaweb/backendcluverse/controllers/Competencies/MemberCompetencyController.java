package com.hexaweb.backendcluverse.controllers.Competencies;

import com.hexaweb.backendcluverse.dto.Competencies         .CompetencyMatchingRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyMatchingResponse;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyGapResponse;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyRequest;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyUpdateRequest;
import com.hexaweb.backendcluverse.dto.Competencies.SpeechAnalyzerHealthResponse;
import com.hexaweb.backendcluverse.dto.Competencies.SpeechAnalyzerSyncResponse;
import com.hexaweb.backendcluverse.services.competencies.MemberCompetencyService;
import com.hexaweb.backendcluverse.services.competencies.SpeechAnalyzerIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/member-competencies")
@RequiredArgsConstructor
public class MemberCompetencyController {

    private final MemberCompetencyService memberCompetencyService;
    private final SpeechAnalyzerIntegrationService speechAnalyzerIntegrationService;

    @PostMapping
    public ResponseEntity<MemberCompetencyResponse> create(@Valid @RequestBody MemberCompetencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberCompetencyService.create(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MemberCompetencyResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(memberCompetencyService.getByUser(userId));
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<MemberCompetencyResponse>> getByClub(@PathVariable Long clubId) {
        return ResponseEntity.ok(memberCompetencyService.getByClub(clubId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberCompetencyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(memberCompetencyService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberCompetencyResponse> update(@PathVariable Long id,
                                                           @Valid @RequestBody MemberCompetencyUpdateRequest request) {
        return ResponseEntity.ok(memberCompetencyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberCompetencyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/endorse")
    public ResponseEntity<MemberCompetencyResponse> endorse(@PathVariable Long id) {
        return ResponseEntity.ok(memberCompetencyService.endorse(id));
    }

    @GetMapping("/{id}/gap")
    public ResponseEntity<MemberCompetencyGapResponse> getGap(@PathVariable Long id) {
        return ResponseEntity.ok(memberCompetencyService.getGap(id));
    }

    @PostMapping("/matching")
    public ResponseEntity<CompetencyMatchingResponse> smartMatching(@Valid @RequestBody CompetencyMatchingRequest request) {
        return ResponseEntity.ok(memberCompetencyService.getSmartMatching(request));
    }

    @GetMapping("/speech-analyzer/health")
    public ResponseEntity<SpeechAnalyzerHealthResponse> getSpeechAnalyzerHealth() {
        return ResponseEntity.ok(speechAnalyzerIntegrationService.getHealth());
    }

    @PostMapping("/{id}/speech-analyzer/sync/{sessionId}")
    public ResponseEntity<SpeechAnalyzerSyncResponse> syncSpeechAnalyzerReport(@PathVariable Long id,
                                                                                @PathVariable String sessionId) {
        return ResponseEntity.ok(speechAnalyzerIntegrationService.syncSessionReportToCompetency(id, sessionId));
    }
}
