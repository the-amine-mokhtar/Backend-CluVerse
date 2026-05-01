package com.hexaweb.backendcluverse.controllers.recrutement;

import com.hexaweb.backendcluverse.services.Recrutement.GroqService;
import com.hexaweb.backendcluverse.services.Recrutement.InterviewPromptService;
import com.hexaweb.backendcluverse.services.Recrutement.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai-interview")
@RequiredArgsConstructor
public class InterviewController {

    private final GroqService groqService;
    private final InterviewSessionService sessionService;
    private final InterviewPromptService promptService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody Map<String, Object> body) {
        String clubName = (String) body.get("clubName");
        String clubDescription = (String) body.get("clubDescription");
        String campaignTitle = (String) body.get("campaignTitle");
        String candidateName = (String) body.get("candidateName");
        String presidentNotes = (String) body.getOrDefault("presidentNotes", "");
        Integer duration = (Integer) body.getOrDefault("duration", 30);
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : 0L;
        Long clubId = body.get("clubId") != null ? Long.valueOf(body.get("clubId").toString()) : 0L;

        String sessionId = sessionService.createSession(
                userId, clubId, "Club", "junior", "Motivation", "FR", duration
        );

        String systemPrompt = promptService.buildInterviewSystemPrompt(
                clubName, clubDescription, campaignTitle, candidateName, duration, presidentNotes
        );

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt +
                "\n\nIMPORTANT: Return JSON only. No markdown. No code blocks."));
        messages.add(Map.of("role", "user", "content",
                "Démarre l'entretien maintenant. Pose la première question. Retourne uniquement du JSON."));

        Map<String, Object> response = groqService.chat(messages, 0.7);

        String say = (String) response.get("say");
        if (say != null) {
            sessionService.saveMessage(sessionId, "recruiter", say, System.currentTimeMillis(), 0);
        }

        Map<String, Object> session = sessionService.getSession(sessionId);
        session.put("hasInitialized", true);
        session.put("systemPrompt", systemPrompt);
        List<Map<String, String>> history = (List<Map<String, String>>) session.get("messages");
        history.add(Map.of("role", "assistant", "content", say != null ? say : ""));
        sessionService.updateSession(sessionId, session);

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("sessionId", sessionId);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/turn")
    public ResponseEntity<Map<String, Object>> turn(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        String candidateText = (String) body.get("candidateText");
        int elapsedSec = body.get("elapsedSec") != null ? (Integer) body.get("elapsedSec") : 0;

        Map<String, Object> session = sessionService.getSession(sessionId);
        String systemPrompt = (String) session.get("systemPrompt");
        List<Map<String, String>> history = (List<Map<String, String>>) session.get("messages");

        sessionService.saveMessage(sessionId, "candidate", candidateText, System.currentTimeMillis(), elapsedSec);
        history.add(Map.of("role", "user", "content", candidateText));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt +
                "\n\nIMPORTANT: Return JSON only. No markdown. No code blocks."));
        messages.addAll(history);

        Map<String, Object> response = groqService.chat(messages, 0.7);

        String say = (String) response.get("say");
        if (say != null) {
            sessionService.saveMessage(sessionId, "recruiter", say, System.currentTimeMillis(), elapsedSec);
            history.add(Map.of("role", "assistant", "content", say));
        }

        sessionService.updateSession(sessionId, session);

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/end")
    public ResponseEntity<Map<String, Object>> end(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        String uniqueLink = (String) body.get("uniqueLink");

        Map<String, Object> session = sessionService.getSession(sessionId);
        List<Map<String, String>> history = (List<Map<String, String>>) session.get("messages");

        List<Map<String, String>> messages = new ArrayList<>(history);
        messages.add(0, Map.of("role", "system", "content",
                "You are a professional recruiter providing final interview evaluation. Respond in French."));
        messages.add(Map.of("role", "user", "content", promptService.buildFinalEvaluationPrompt()));

        Map<String, Object> report = groqService.chat(messages, 0.2);

        String impression = (String) report.getOrDefault("recruiter_impression", "Rejeter");
        int overallScore = report.get("overall_score") != null ?
                Integer.parseInt(report.get("overall_score").toString()) : 0;
        List<String> strengths = (List<String>) report.getOrDefault("strengths", List.of());
        List<String> weaknesses = (List<String>) report.getOrDefault("weaknesses", List.of());
        String summary = (String) report.getOrDefault("summary", "");

        sessionService.saveReport(sessionId, impression, overallScore, strengths, weaknesses, summary,
                report.toString());

        // Mettre à jour le statut InterviewConfig
        if (uniqueLink != null) {
            // Le service mettra à jour via repository
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("finalReport", report);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        Map<String, Object> session = sessionService.getSession(sessionId);
        return ResponseEntity.ok(session);
    }
}