package com.hexaweb.backendcluverse.services.competencies;

import com.fasterxml.jackson.databind.JsonNode;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse;
import com.hexaweb.backendcluverse.dto.Competencies.SpeechAnalyzerHealthResponse;
import com.hexaweb.backendcluverse.dto.Competencies.SpeechAnalyzerSyncResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class SpeechAnalyzerIntegrationService {

    private final WebClient webClient;
    private final MemberCompetencyService memberCompetencyService;

    public SpeechAnalyzerIntegrationService(WebClient.Builder webClientBuilder,
                                            MemberCompetencyService memberCompetencyService,
                                            @Value("${speech.analyzer.base-url:http://localhost:8001}") String speechAnalyzerBaseUrl) {
        this.memberCompetencyService = memberCompetencyService;
        this.webClient = webClientBuilder.baseUrl(speechAnalyzerBaseUrl).build();
    }

    public SpeechAnalyzerHealthResponse getHealth() {
        try {
            JsonNode healthNode = webClient.post()
                    .uri("/health")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new ResponseStatusException(
                                    HttpStatus.BAD_GATEWAY,
                                    "Speech analyzer health call failed: " + body
                            ))))
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(12));

            if (healthNode == null) {
                return new SpeechAnalyzerHealthResponse("degraded", false, "unknown");
            }

            return new SpeechAnalyzerHealthResponse(
                    healthNode.path("status").asText("degraded"),
                    healthNode.path("whisper_loaded").asBoolean(false),
                    healthNode.path("model_version").asText("unknown")
            );
        } catch (RuntimeException ex) {
            log.warn("Speech analyzer health unreachable, returning degraded state: {}", ex.getMessage());
            return new SpeechAnalyzerHealthResponse("degraded", false, "unavailable");
        }
    }

    public SpeechAnalyzerSyncResponse syncSessionReportToCompetency(Long memberCompetencyId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required");
        }

                JsonNode reportNode;
                try {
                        reportNode = fetchReport(sessionId, Duration.ofSeconds(20));
                } catch (RuntimeException ex) {
                        if (!isTimeout(ex)) {
                                throw ex;
                        }
                        log.warn("Speech analyzer report timed out for sessionId={}, retrying once", sessionId);
                        reportNode = fetchReport(sessionId, Duration.ofSeconds(45));
                }

        if (reportNode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty report response from speech analyzer");
        }

        JsonNode scoreNode = reportNode.path("score");
        if (scoreNode.isMissingNode()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Speech analyzer report does not contain score");
        }

        double speechScore = scoreNode.path("global_score").asDouble(Double.NaN);
        if (Double.isNaN(speechScore)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Speech analyzer score is invalid");
        }

        int normalizedScore = Math.max(0, Math.min(100, (int) Math.round(speechScore)));
        MemberCompetencyResponse updatedCompetency = memberCompetencyService.applySpeechAnalyzerScore(memberCompetencyId, normalizedScore);

        String speechLevel = scoreNode.path("level").asText("INTERMEDIATE");
        String feedback = reportNode.path("feedback").asText("");

        log.info("Speech analyzer report synced for sessionId={} on memberCompetencyId={} score={}",
                sessionId, memberCompetencyId, normalizedScore);

        return new SpeechAnalyzerSyncResponse(
                sessionId,
                BigDecimal.valueOf(speechScore).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                speechLevel,
                feedback,
                updatedCompetency
        );
    }

        public String buildSessionReportSummary(String sessionId) {
                if (sessionId == null || sessionId.isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required");
                }

                JsonNode reportNode;
                try {
                        reportNode = fetchReport(sessionId, Duration.ofSeconds(20));
                } catch (RuntimeException ex) {
                        if (!isTimeout(ex)) {
                                throw ex;
                        }
                        log.warn("Speech analyzer report timed out for sessionId={}, retrying once", sessionId);
                        reportNode = fetchReport(sessionId, Duration.ofSeconds(45));
                }

                if (reportNode == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty report response from speech analyzer");
                }

                JsonNode scoreNode = reportNode.path("score");
                double globalScore = scoreNode.path("global_score").asDouble(0.0);
                String level = scoreNode.path("level").asText("INTERMEDIATE");
                String feedback = reportNode.path("feedback").asText("").trim();

                String header = String.format("AI Session Report: score=%.2f/100, level=%s", globalScore, level);
                if (feedback.isBlank()) {
                        return header;
                }

                return header + "\n" + feedback;
        }

        private JsonNode fetchReport(String sessionId, Duration timeout) {
                return webClient.post()
                                .uri("/report/{sessionId}", sessionId)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                                                HttpStatus.BAD_GATEWAY,
                                                                "Speech analyzer report call failed: " + body
                                                ))))
                                .bodyToMono(JsonNode.class)
                                .block(timeout);
        }

        private boolean isTimeout(Throwable throwable) {
                Throwable current = throwable;
                while (current != null) {
                        if (current instanceof TimeoutException) {
                                return true;
                        }
                        current = current.getCause();
                }
                return false;
        }
}
