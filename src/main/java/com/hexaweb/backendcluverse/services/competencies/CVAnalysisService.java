package com.hexaweb.backendcluverse.services.competencies;

import com.hexaweb.backendcluverse.dto.Competencies.CVAnalysisResponse;
import com.hexaweb.backendcluverse.dto.Competencies.LearningPathRequest;
import com.hexaweb.backendcluverse.dto.Competencies.LearningPathResponse;
import com.hexaweb.backendcluverse.dto.Competencies.LevelUpdateRequest;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyRequest;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.competencies.Competency;
import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import com.hexaweb.backendcluverse.enumerations.UpdateSource;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencyRepository;
import com.hexaweb.backendcluverse.repositories.Competencies.MemberCompetencyRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CVAnalysisService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final MemberCompetencyService memberCompetencyService;
    private final CompetencyRepository competencyRepository;
    private final MemberCompetencyRepository memberCompetencyRepository;
    private final Tika tika = new Tika();

    public CVAnalysisService(WebClient.Builder webClientBuilder,
                             UserRepository userRepository,
                             MemberCompetencyService memberCompetencyService,
                             CompetencyRepository competencyRepository,
                             MemberCompetencyRepository memberCompetencyRepository,
                             @Value("${speech.analyzer.base-url:http://localhost:8001}") String speechAnalyzerBaseUrl) {
        this.userRepository = userRepository;
        this.memberCompetencyService = memberCompetencyService;
        this.competencyRepository = competencyRepository;
        this.memberCompetencyRepository = memberCompetencyRepository;
        this.webClient = webClientBuilder.baseUrl(speechAnalyzerBaseUrl).build();
    }

    @Transactional
    public CVAnalysisResponse analyzeAndApplyCV(Long userId, Long clubId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate file
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CV file is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".pdf") && 
            !filename.toLowerCase().endsWith(".docx") && 
            !filename.toLowerCase().endsWith(".doc") && 
            !filename.toLowerCase().endsWith(".txt"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Unsupported file format. Please upload PDF, DOCX, DOC, or TXT files only.");
        }

        String text;
        try {
            text = tika.parseToString(file.getInputStream());
            log.info("Successfully extracted {} characters from CV: {}", text.length(), filename);
        } catch (Exception e) {
            log.error("Failed to parse CV file: {}", filename, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Failed to extract text from CV. Please ensure the file is not corrupted and try again.");
        }

        CVAnalysisResponse analysis = webClient.post()
                .uri("/cv/analyze")
                .bodyValue(Map.of("text", text))
                .retrieve()
                .bodyToMono(CVAnalysisResponse.class)
                .block();

        if (analysis == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI analysis failed");
        }

        // Apply competencies
        for (CVAnalysisResponse.CompetencyImpact impact : analysis.getSuggested_competencies()) {
            Optional<Competency> compOpt = competencyRepository.findByNameAndClubId(impact.getName(), clubId);
            Competency comp;
            if (compOpt.isPresent()) {
                comp = compOpt.get();
            } else {
                // Create new competency if not found
                comp = Competency.builder()
                        .name(impact.getName())
                        .category(impact.getCategory() != null ? CompetencyType.valueOf(impact.getCategory()) : CompetencyType.TECHNICAL)
                        .clubId(clubId)
                        .description("Automatiquement créé par l'IA lors de l'analyse du CV")
                        .build();
                comp = competencyRepository.save(comp);
                log.info("Created new global competency: {} for clubId={}", comp.getName(), clubId);
            }

            try {
                boolean exists = memberCompetencyRepository.existsByUserIdAndSkillId(userId, comp.getId());
                
                if (exists) {
                    // Update existing competency level
                    LevelUpdateRequest levelRequest = new LevelUpdateRequest();
                    levelRequest.setNewLevel(impact.getLevel());
                    levelRequest.setSource(UpdateSource.AI_COACH);
                    memberCompetencyService.updateLevel(userId, comp.getId(), levelRequest);
                    log.info("Updated existing competency {} for userId={}", impact.getName(), userId);
                } else {
                    // Assign new competency
                    MemberCompetencyRequest request = new MemberCompetencyRequest();
                    request.setUserId(userId);
                    request.setSkillId(comp.getId());
                    request.setCurrentLevel(impact.getLevel());
                    request.setTargetLevel(Math.min(5, impact.getLevel() + 1));
                    request.setLastUpdatedBy(UpdateSource.AI_COACH);
                    
                    memberCompetencyService.assign(request);
                    log.info("Assigned new competency {} for userId={}", impact.getName(), userId);
                }
            } catch (Exception e) {
                log.warn("Could not process competency {} : {}", impact.getName(), e.getMessage());
            }
        }

        return analysis;
    }

    public LearningPathResponse generateLearningPath(String skillName, int targetLevel) {
        return webClient.post()
                .uri("/learning-path/generate")
                .bodyValue(new LearningPathRequest(skillName, targetLevel))
                .retrieve()
                .bodyToMono(LearningPathResponse.class)
                .block();
    }
}
