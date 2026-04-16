package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.election.InterviewConfig;
import com.hexaweb.backendcluverse.repositories.InterviewConfigRepository;
import com.hexaweb.backendcluverse.repositories.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterviewConfigService {

    private final InterviewConfigRepository interviewConfigRepository;
    private final ApplicationRepository applicationRepository;

    public InterviewConfig createConfig(Long applicationId, InterviewConfig config) {
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        config.setApplication(application);
        return interviewConfigRepository.save(config);
    }

    public InterviewConfig getByUniqueLink(String uniqueLink) {
        return interviewConfigRepository.findByUniqueLink(uniqueLink)
                .orElseThrow(() -> new RuntimeException("Interview not found"));
    }

    public InterviewConfig getByApplicationId(Long applicationId) {
        return interviewConfigRepository.findTopByApplicationIdOrderByCreatedAtDesc(applicationId)
                .orElseThrow(() -> new RuntimeException("Interview config not found"));
    }

    public InterviewConfig updateStatus(String uniqueLink, String status) {
        InterviewConfig config = getByUniqueLink(uniqueLink);
        config.setStatus(status);
        return interviewConfigRepository.save(config);
    }

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getInterviewResult(Long applicationId) {
        InterviewConfig config = interviewConfigRepository
                .findTopByApplicationIdOrderByCreatedAtDesc(applicationId)
                .orElseThrow(() -> new RuntimeException("Interview config not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("interviewConfig", config);
        result.put("uniqueLink", config.getUniqueLink());
        result.put("status", config.getStatus());

        // Lire le rapport depuis MySQL via JDBC
        try {
            Map<String, Object> report = jdbcTemplate.queryForMap(
                    "SELECT s.status as sessionStatus, r.impression, r.overall_score, " +
                            "r.strengths, r.weaknesses, r.summary " +
                            "FROM interview_sessions s " +
                            "LEFT JOIN interview_final_reports r ON r.session_id = s.id " +
                            "WHERE s.club_id = ? AND s.status = 'ended' " +
                            "ORDER BY s.created_at DESC LIMIT 1",
                    config.getApplication().getRecruitmentCampaign().getClub().getId()
            );
            result.put("report", report);
            result.put("status", "TERMINE");
        } catch (Exception e) {
            result.put("report", null);
        }

        return result;
    }
}