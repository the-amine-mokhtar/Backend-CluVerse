package com.hexaweb.backendcluverse.controllers.Competencies;

import com.hexaweb.backendcluverse.dto.Competencies.AdminCompetencySessionsResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionCancelRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionCloseRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionParticipantResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionRescheduleRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionScheduleRequest;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencySessionsResponse;
import com.hexaweb.backendcluverse.dto.Competencies.SessionAttendanceUpdate;
import com.hexaweb.backendcluverse.dto.Competencies.SessionParticipantInviteRequest;
import com.hexaweb.backendcluverse.services.competencies.CompetencySessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/competency-sessions")
@RequiredArgsConstructor
public class CompetencySessionController {

    private final CompetencySessionService competencySessionService;

    @GetMapping("/admin")
    public ResponseEntity<AdminCompetencySessionsResponse> getAdminSessions(@RequestParam Long clubId,
                                                                            @RequestParam(required = false) String status,
                                                                            @RequestParam(required = false) Long competencyId,
                                                                            @RequestParam(required = false) Long coachUserId,
                                                                            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(competencySessionService.getAdminSessions(clubId, status, competencyId, coachUserId, search));
    }

    @GetMapping("/member")
    public ResponseEntity<MemberCompetencySessionsResponse> getMemberSessions(@RequestParam Long clubId,
                                                                               @RequestParam Long userId) {
        return ResponseEntity.ok(competencySessionService.getMemberSessions(clubId, userId));
    }

    @PostMapping("/schedule")
    public ResponseEntity<CompetencySessionResponse> schedule(@Valid @RequestBody CompetencySessionScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competencySessionService.schedule(request));
    }

    @PostMapping("/{sessionId}/close")
    public ResponseEntity<CompetencySessionResponse> closeSession(@PathVariable Long sessionId,
                                                                  @Valid @RequestBody CompetencySessionCloseRequest request) {
        return ResponseEntity.ok(competencySessionService.closeSession(sessionId, request));
    }

    @PatchMapping("/{sessionId}/cancel")
    public ResponseEntity<CompetencySessionResponse> cancelSession(@PathVariable Long sessionId,
                                                                   @RequestBody(required = false) CompetencySessionCancelRequest request) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(competencySessionService.cancelSession(sessionId, reason));
    }

    @PatchMapping("/{sessionId}/reschedule")
    public ResponseEntity<CompetencySessionResponse> rescheduleSession(@PathVariable Long sessionId,
                                                                       @Valid @RequestBody CompetencySessionRescheduleRequest request) {
        return ResponseEntity.ok(competencySessionService.rescheduleSession(sessionId, request));
    }

    @GetMapping("/{sessionId}/participants")
    public ResponseEntity<List<CompetencySessionParticipantResponse>> getParticipants(@PathVariable Long sessionId) {
        return ResponseEntity.ok(competencySessionService.getSessionParticipants(sessionId));
    }

    @PostMapping("/{sessionId}/participants/invite")
    public ResponseEntity<List<CompetencySessionParticipantResponse>> inviteParticipants(@PathVariable Long sessionId,
                                                                                          @Valid @RequestBody SessionParticipantInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(competencySessionService.inviteParticipants(sessionId, request.getParticipantUserIds()));
    }

    @PatchMapping("/{sessionId}/participants/attendance")
    public ResponseEntity<List<CompetencySessionParticipantResponse>> updateAttendance(@PathVariable Long sessionId,
                                                                                        @Valid @RequestBody List<SessionAttendanceUpdate> attendance) {
        return ResponseEntity.ok(competencySessionService.updateAttendance(sessionId, attendance));
    }

    @GetMapping("/{sessionId}/report")
    public ResponseEntity<CompetencySessionResponse> getReport(@PathVariable Long sessionId,
                                                               @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(competencySessionService.getReport(sessionId, userId));
    }
}
