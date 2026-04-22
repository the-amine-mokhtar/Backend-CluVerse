package com.hexaweb.backendcluverse.services.competencies;

import com.hexaweb.backendcluverse.dto.Competencies.AdminCompetencySessionsResponse;
import com.hexaweb.backendcluverse.dto.Competencies.AdminSessionMetricsResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionCloseRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionParticipantResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionRescheduleRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionScheduleRequest;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencySessionsResponse;
import com.hexaweb.backendcluverse.dto.Competencies.MemberSessionMetricsResponse;
import com.hexaweb.backendcluverse.dto.Competencies.SessionAttendanceUpdate;
import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.competencies.Competency;
import com.hexaweb.backendcluverse.entities.competencies.CompetencySession;
import com.hexaweb.backendcluverse.entities.competencies.CompetencySessionParticipant;
import com.hexaweb.backendcluverse.enumerations.SessionStatus;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencyRepository;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencySessionParticipantRepository;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencySessionRepository;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompetencySessionService {

    private final CompetencySessionRepository sessionRepository;
    private final CompetencySessionParticipantRepository participantRepository;
    private final CompetencyRepository competencyRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final GoogleCalendarService googleCalendarService;
    private final MemberCompetencyService memberCompetencyService;
    private final SpeechAnalyzerIntegrationService speechAnalyzerIntegrationService;

    @Transactional(readOnly = true)
    public AdminCompetencySessionsResponse getAdminSessions(Long clubId,
                                                            String status,
                                                            Long competencyId,
                                                            Long coachUserId,
                                                            String search) {
        List<CompetencySession> sessions = sessionRepository.findByClubIdOrderByStartsAtDesc(clubId);

        sessions = sessions.stream()
                .filter(session -> filterByStatus(session, status))
                .filter(session -> competencyId == null || competencyId.equals(session.getCompetencyId()))
                .filter(session -> coachUserId == null || coachUserId.equals(session.getCoachUserId()))
                .filter(session -> {
                    if (search == null || search.isBlank()) {
                        return true;
                    }
                    String normalized = search.toLowerCase();
                    return session.getTitle().toLowerCase().contains(normalized);
                })
                .toList();

        List<CompetencySessionResponse> mapped = mapSessions(sessions, null, false);
        AdminSessionMetricsResponse metrics = buildAdminMetrics(sessionRepository.findByClubIdOrderByStartsAtDesc(clubId));

        return AdminCompetencySessionsResponse.builder()
                .metrics(metrics)
                .sessions(mapped)
                .build();
    }

    @Transactional(readOnly = true)
    public MemberCompetencySessionsResponse getMemberSessions(Long clubId, Long userId) {
        if (!membershipRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not member of this club");
        }

        List<CompetencySessionParticipant> links = participantRepository.findByClubIdAndUserId(clubId, userId);
        List<CompetencySession> sessions = links.stream()
                .map(CompetencySessionParticipant::getSession)
                .toList();

        LocalDateTime now = LocalDateTime.now();

        List<CompetencySessionResponse> upcoming = mapSessions(
                sessions.stream()
                        .filter(session -> {
                            SessionStatus derived = resolveDisplayStatus(session);
                            return derived == SessionStatus.SCHEDULED || derived == SessionStatus.ONGOING;
                        })
                        .sorted((a, b) -> a.getStartsAt().compareTo(b.getStartsAt()))
                        .toList(),
                userId,
                false
        );

        List<CompetencySessionResponse> past = mapSessions(
                sessions.stream()
                        .filter(session -> {
                            SessionStatus derived = resolveDisplayStatus(session);
                            if (derived == SessionStatus.COMPLETED || derived == SessionStatus.CANCELLED) {
                                return true;
                            }
                            return session.getStartsAt().isBefore(now) && derived == SessionStatus.SCHEDULED;
                        })
                        .sorted((a, b) -> b.getStartsAt().compareTo(a.getStartsAt()))
                        .toList(),
                userId,
                false
        );

        long completed = past.stream().filter(item -> item.getStatus() == SessionStatus.COMPLETED).count();
        long workedCompetencies = past.stream()
                .filter(item -> item.getStatus() == SessionStatus.COMPLETED && Boolean.TRUE.equals(item.getAttended()))
                .map(CompetencySessionResponse::getCompetencyId)
                .distinct()
                .count();

        MemberSessionMetricsResponse metrics = MemberSessionMetricsResponse.builder()
                .upcomingSessions(upcoming.size())
                .completedSessions(completed)
                .competenciesWorked(workedCompetencies)
                .build();

        return MemberCompetencySessionsResponse.builder()
                .metrics(metrics)
                .upcoming(upcoming)
                .past(past)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CompetencySessionParticipantResponse> getSessionParticipants(Long sessionId) {
        CompetencySession session = findSession(sessionId);
        return mapParticipants(session, buildUserMap(extractParticipantIds(session)));
    }

    @Transactional(readOnly = true)
    public CompetencySessionResponse getReport(Long sessionId, Long userId) {
        CompetencySession session = findSession(sessionId);
        return mapSession(session, buildCompetencyMap(Set.of(session.getCompetencyId())), buildUserMap(extractRelevantUserIds(session)), userId, true);
    }

    @Transactional
    public CompetencySessionResponse schedule(CompetencySessionScheduleRequest request) {
        Competency competency = competencyRepository.findById(request.getCompetencyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competency not found"));

        if (!competency.getClubId().equals(request.getClubId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competency does not belong to this club");
        }

        ensureMembership(request.getClubId(), request.getCoachUserId());
        for (Long userId : request.getParticipantUserIds()) {
            ensureMembership(request.getClubId(), userId);
        }

        User coach = userRepository.findById(request.getCoachUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coach not found"));

        String meetLink = request.getMeetLink();
        if (meetLink == null || meetLink.isBlank()) {
            try {
                meetLink = googleCalendarService.createMeetEvent(coach, request, competency.getName());
            } catch (ResponseStatusException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Meet link missing. Connect coach Google Calendar or provide meetLink manually.",
                        ex
                );
            }
        }

        CompetencySession session = new CompetencySession();
        session.setClubId(request.getClubId());
        session.setCompetencyId(request.getCompetencyId());
        session.setCoachUserId(request.getCoachUserId());
        session.setTitle(request.getTitle().trim());
        session.setMeetLink(meetLink);
        session.setStartsAt(request.getStartsAt());
        session.setStatus(SessionStatus.SCHEDULED);

        CompetencySession saved = sessionRepository.save(session);

        List<CompetencySessionParticipant> participants = request.getParticipantUserIds().stream()
                .distinct()
                .map(userId -> CompetencySessionParticipant.builder()
                        .session(saved)
                        .userId(userId)
                        .attended(null)
                        .build())
                .toList();

        participantRepository.saveAll(participants);
        saved.setParticipants(new ArrayList<>(participants));

        return mapSession(saved, buildCompetencyMap(Set.of(saved.getCompetencyId())), buildUserMap(extractRelevantUserIds(saved)), null, false);
    }

    @Transactional
    public CompetencySessionResponse closeSession(Long sessionId, CompetencySessionCloseRequest request) {
        CompetencySession session = findSession(sessionId);

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled session cannot be closed");
        }

        List<CompetencySessionParticipant> participants = updateAttendanceInternal(
            sessionId,
            request.getAttendance() == null ? List.of() : request.getAttendance()
        );

        session.setEndsAt(LocalDateTime.now());
        session.setStatus(SessionStatus.COMPLETED);
        session.setReportSummary(resolveReportSummary(request));

        CompetencySession saved = sessionRepository.save(session);
        saved.setParticipants(participants);

        // On closing a session, attended members progress by one level toward target.
        for (CompetencySessionParticipant participant : participants) {
            if (Boolean.TRUE.equals(participant.getAttended())) {
                memberCompetencyService.progressFromLiveSession(participant.getUserId(), session.getCompetencyId());
            }
        }

        return mapSession(saved, buildCompetencyMap(Set.of(saved.getCompetencyId())), buildUserMap(extractRelevantUserIds(saved)), null, true);
    }

    @Transactional
    public List<CompetencySessionParticipantResponse> inviteParticipants(Long sessionId, List<Long> participantUserIds) {
        CompetencySession session = findSession(sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot invite participants to completed or cancelled session");
        }

        List<Long> incoming = participantUserIds == null ? List.of() : participantUserIds.stream().distinct().toList();
        if (incoming.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participantUserIds cannot be empty");
        }

        for (Long userId : incoming) {
            ensureMembership(session.getClubId(), userId);
        }

        List<CompetencySessionParticipant> existing = participantRepository.findBySession_Id(sessionId);
        Set<Long> existingUserIds = existing.stream().map(CompetencySessionParticipant::getUserId).collect(Collectors.toSet());

        List<CompetencySessionParticipant> toCreate = incoming.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .map(userId -> CompetencySessionParticipant.builder()
                        .session(session)
                        .userId(userId)
                        .attended(null)
                        .build())
                .toList();

        if (!toCreate.isEmpty()) {
            participantRepository.saveAll(toCreate);
            existing = new ArrayList<>(existing);
            existing.addAll(toCreate);
        }

        session.setParticipants(existing);
        return mapParticipants(session, buildUserMap(extractParticipantIds(session)));
    }

    @Transactional
    public List<CompetencySessionParticipantResponse> updateAttendance(Long sessionId, List<SessionAttendanceUpdate> attendance) {
        CompetencySession session = findSession(sessionId);
        List<CompetencySessionParticipant> updated = updateAttendanceInternal(sessionId, attendance);
        session.setParticipants(updated);
        return mapParticipants(session, buildUserMap(extractParticipantIds(session)));
    }

    private List<CompetencySessionParticipant> updateAttendanceInternal(Long sessionId, List<SessionAttendanceUpdate> attendance) {
        Map<Long, SessionAttendanceUpdate> attendanceMap = attendance == null
                ? Map.of()
                : attendance.stream().collect(Collectors.toMap(SessionAttendanceUpdate::getUserId, item -> item, (left, right) -> right));

        List<CompetencySessionParticipant> participants = participantRepository.findBySession_Id(sessionId);
        for (CompetencySessionParticipant participant : participants) {
            SessionAttendanceUpdate update = attendanceMap.get(participant.getUserId());
            if (update != null) {
                participant.setAttended(update.getAttended());
            }
        }
        return participantRepository.saveAll(participants);
    }

    private String resolveReportSummary(CompetencySessionCloseRequest request) {
        if (request.getReportSummary() != null && !request.getReportSummary().isBlank()) {
            return request.getReportSummary().trim();
        }

        boolean shouldAutoGenerate = Boolean.TRUE.equals(request.getAutoGenerateReport())
                && request.getSpeechSessionId() != null
                && !request.getSpeechSessionId().isBlank();

        if (!shouldAutoGenerate) {
            return null;
        }

        try {
            return speechAnalyzerIntegrationService.buildSessionReportSummary(request.getSpeechSessionId().trim());
        } catch (RuntimeException ex) {
            return "Rapport IA indisponible pour cette session (service speech-analyzer inaccessible).";
        }
    }

    @Transactional
    public CompetencySessionResponse cancelSession(Long sessionId, String reason) {
        CompetencySession session = findSession(sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed session cannot be cancelled");
        }

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is already cancelled");
        }

        session.setStatus(SessionStatus.CANCELLED);
        session.setEndsAt(LocalDateTime.now());
        session.setCancellationReason(reason);

        CompetencySession saved = sessionRepository.save(session);
        return mapSession(saved, buildCompetencyMap(Set.of(saved.getCompetencyId())), buildUserMap(extractRelevantUserIds(saved)), null, false);
    }

    @Transactional
    public CompetencySessionResponse rescheduleSession(Long sessionId, CompetencySessionRescheduleRequest request) {
        CompetencySession session = findSession(sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only scheduled or ongoing sessions can be rescheduled");
        }

        LocalDateTime previousStart = session.getStartsAt();
        LocalDateTime previousEnd = session.getEndsAt();

        session.setStartsAt(request.getStartsAt());
        if (previousEnd != null && previousStart != null && previousEnd.isAfter(previousStart)) {
            Duration duration = Duration.between(previousStart, previousEnd);
            session.setEndsAt(request.getStartsAt().plus(duration));
        }

        CompetencySession saved = sessionRepository.save(session);
        return mapSession(saved, buildCompetencyMap(Set.of(saved.getCompetencyId())), buildUserMap(extractRelevantUserIds(saved)), null, false);
    }

    private void ensureMembership(Long clubId, Long userId) {
        Membership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User " + userId + " does not belong to club"));

        if (!membership.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User " + userId + " membership is inactive");
        }
    }

    private CompetencySession findSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    private boolean filterByStatus(CompetencySession session, String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return true;
        }

        try {
            SessionStatus wanted = SessionStatus.valueOf(status.trim().toUpperCase());
            return resolveDisplayStatus(session) == wanted;
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    private SessionStatus resolveDisplayStatus(CompetencySession session) {
        if (session.getStatus() == SessionStatus.SCHEDULED) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = session.getStartsAt();
            LocalDateTime end = session.getEndsAt();

            if (start != null && start.isBefore(now) && (end == null || end.isAfter(now))) {
                return SessionStatus.ONGOING;
            }
        }

        return session.getStatus();
    }

    private AdminSessionMetricsResponse buildAdminMetrics(List<CompetencySession> allSessions) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        long upcoming = allSessions.stream()
                .filter(session -> {
                    SessionStatus status = resolveDisplayStatus(session);
                    return status == SessionStatus.SCHEDULED || status == SessionStatus.ONGOING;
                })
                .count();

        long completedThisMonth = allSessions.stream()
                .filter(session -> session.getStatus() == SessionStatus.COMPLETED)
                .filter(session -> session.getEndsAt() != null
                        && session.getEndsAt().getMonth() == today.getMonth()
                        && session.getEndsAt().getYear() == today.getYear())
                .count();

        long cancelled = allSessions.stream()
                .filter(session -> session.getStatus() == SessionStatus.CANCELLED)
                .count();

        return AdminSessionMetricsResponse.builder()
                .totalSessions(allSessions.size())
                .upcomingSessions(upcoming)
                .completedThisMonth(completedThisMonth)
                .cancelledSessions(cancelled)
                .build();
    }

    private List<CompetencySessionResponse> mapSessions(List<CompetencySession> sessions, Long viewerUserId, boolean includeParticipants) {
        Set<Long> competencyIds = sessions.stream().map(CompetencySession::getCompetencyId).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
        for (CompetencySession session : sessions) {
            userIds.add(session.getCoachUserId());
            userIds.addAll(extractParticipantIds(session));
        }

        Map<Long, Competency> competencyMap = buildCompetencyMap(competencyIds);
        Map<Long, User> userMap = buildUserMap(userIds);

        return sessions.stream()
                .map(session -> mapSession(session, competencyMap, userMap, viewerUserId, includeParticipants))
                .toList();
    }

    private CompetencySessionResponse mapSession(CompetencySession session,
                                                 Map<Long, Competency> competencyMap,
                                                 Map<Long, User> userMap,
                                                 Long viewerUserId,
                                                 boolean includeParticipants) {
        Competency competency = competencyMap.get(session.getCompetencyId());
        User coach = userMap.get(session.getCoachUserId());

        List<CompetencySessionParticipant> participants = getParticipants(session);

        Boolean attended = null;
        if (viewerUserId != null) {
            CompetencySessionParticipant match = participants.stream()
                .filter(participant -> participant.getUserId().equals(viewerUserId))
                .findFirst()
                .orElse(null);
            attended = match == null ? null : match.getAttended();
        }

        List<CompetencySessionParticipantResponse> participantResponses = includeParticipants
                ? mapParticipants(session, userMap)
                : null;

        String coachName = coach == null
                ? "Unknown"
                : ((coach.getFirstName() == null ? "" : coach.getFirstName()) + " " + (coach.getLastName() == null ? "" : coach.getLastName())).trim();

        return CompetencySessionResponse.builder()
                .id(session.getId())
                .clubId(session.getClubId())
                .competencyId(session.getCompetencyId())
                .competencyName(competency == null ? "Unknown" : competency.getName())
                .title(session.getTitle())
                .coachUserId(session.getCoachUserId())
                .coachName(coachName.isBlank() ? "Unknown" : coachName)
                .meetLink(session.getMeetLink())
                .startsAt(session.getStartsAt())
                .endsAt(session.getEndsAt())
                .status(resolveDisplayStatus(session))
                .participantCount(participants.size())
                .reportSummary(session.getReportSummary())
                .cancellationReason(session.getCancellationReason())
                .attended(attended)
                .participants(participantResponses)
                .build();
    }

    private List<CompetencySessionParticipantResponse> mapParticipants(CompetencySession session, Map<Long, User> userMap) {
        return getParticipants(session).stream()
                .map(participant -> {
                    User user = userMap.get(participant.getUserId());
                    String fullName = user == null
                            ? "Unknown"
                            : ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();

                    return CompetencySessionParticipantResponse.builder()
                            .userId(participant.getUserId())
                            .fullName(fullName)
                            .email(user == null ? "" : user.getEmail())
                            .attended(participant.getAttended())
                            .build();
                })
                .toList();
    }

    private List<CompetencySessionParticipant> getParticipants(CompetencySession session) {
        if (session.getParticipants() != null && !session.getParticipants().isEmpty()) {
            return session.getParticipants();
        }
        List<CompetencySessionParticipant> loaded = participantRepository.findBySession_Id(session.getId());
        session.setParticipants(loaded);
        return loaded;
    }

    private Set<Long> extractParticipantIds(CompetencySession session) {
        return getParticipants(session).stream().map(CompetencySessionParticipant::getUserId).collect(Collectors.toSet());
    }

    private Set<Long> extractRelevantUserIds(CompetencySession session) {
        Set<Long> userIds = new HashSet<>(extractParticipantIds(session));
        userIds.add(session.getCoachUserId());
        return userIds;
    }

    private Map<Long, Competency> buildCompetencyMap(Set<Long> competencyIds) {
        if (competencyIds == null || competencyIds.isEmpty()) {
            return new HashMap<>();
        }

        return competencyRepository.findAllById(competencyIds).stream()
                .collect(Collectors.toMap(Competency::getId, item -> item));
    }

    private Map<Long, User> buildUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, item -> item));
    }
}
