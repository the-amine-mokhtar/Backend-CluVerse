package com.hexaweb.backendcluverse.services.Recrutement;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    // Cache sessions en mémoire (comme Next.js)
    private final Map<String, Map<String, Object>> sessionCache = new ConcurrentHashMap<>();

    public String createSession(Long userId, Long clubId, String role, String level,
                                String interviewType, String language, Integer duration) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO interview_sessions (id, created_at, user_id, club_id, role, level, " +
                        "interview_type, language, duration_minutes, status) VALUES (?, NOW(), ?, ?, ?, ?, ?, ?, ?, 'running')",
                id, userId, clubId, role, level, interviewType, language, duration
        );

        Map<String, Object> session = new HashMap<>();
        session.put("id", id);
        session.put("hasInitialized", false);
        session.put("messages", new ArrayList<>());
        sessionCache.put(id, session);

        return id;
    }

    public void saveMessage(String sessionId, String role, String text, long timestampMs, int elapsedSec) {
        String msgId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO interview_messages (id, session_id, role, text, timestamp_ms, elapsed_sec, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                msgId, sessionId, role, text, timestampMs, elapsedSec
        );
    }

    public void saveReport(String sessionId, String impression, int overallScore,
                           List<String> strengths, List<String> weaknesses, String summary, String rawOutput) {
        try {
            String strengthsJson = objectMapper.writeValueAsString(strengths);
            String weaknessesJson = objectMapper.writeValueAsString(weaknesses);

            jdbcTemplate.update(
                    "UPDATE interview_sessions SET status='ended', ended_at=NOW(), " +
                            "recruiter_impression=?, overall_score=? WHERE id=?",
                    impression, overallScore, sessionId
            );

            jdbcTemplate.update(
                    "INSERT INTO interview_final_reports (session_id, impression, overall_score, strengths, " +
                            "weaknesses, summary, raw_model_output, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) " +
                            "ON DUPLICATE KEY UPDATE impression=VALUES(impression), overall_score=VALUES(overall_score), " +
                            "strengths=VALUES(strengths), weaknesses=VALUES(weaknesses), summary=VALUES(summary), " +
                            "raw_model_output=VALUES(raw_model_output)",
                    sessionId, impression, overallScore, strengthsJson, weaknessesJson, summary, rawOutput
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to save report: " + e.getMessage());
        }
    }

    public Map<String, Object> getSession(String sessionId) {
        return sessionCache.computeIfAbsent(sessionId, id -> {
            Map<String, Object> s = new HashMap<>();
            s.put("id", id);
            s.put("hasInitialized", false);
            s.put("messages", new ArrayList<>());
            return s;
        });
    }

    public void updateSession(String sessionId, Map<String, Object> session) {
        sessionCache.put(sessionId, session);
    }
}