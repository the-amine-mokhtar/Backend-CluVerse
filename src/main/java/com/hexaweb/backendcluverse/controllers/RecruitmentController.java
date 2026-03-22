package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.AnswerDto;
import com.hexaweb.backendcluverse.dto.ApplicationSubmissionDto;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.recruitement.*;
import com.hexaweb.backendcluverse.repositories.*;
import com.hexaweb.backendcluverse.enumerations.ApplicationStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recruitment")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentCampaignRepository campaignRepository;
    private final CampaignQuestionRepository questionRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAnswerRepository answerRepository;
    private final ClubRepository clubRepository;

    @PostMapping("/campaigns")
    public ResponseEntity<RecruitmentCampaign> createCampaign(@RequestBody RecruitmentCampaign campaign,
                                                              @RequestParam Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found"));

        campaign.setClub(club);
        campaign.setPublicLink(UUID.randomUUID().toString().substring(0, 8) + "-" + club.getName().toLowerCase().replaceAll("\\s+", "-"));
        campaign.setActive(true);

        return ResponseEntity.ok(campaignRepository.save(campaign));
    }

    @GetMapping("/campaigns/club/{clubId}")
    public ResponseEntity<List<RecruitmentCampaign>> getCampaignsByClub(@PathVariable Long clubId) {
        List<RecruitmentCampaign> campaigns = campaignRepository.findByClubId(clubId);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<RecruitmentCampaign> getCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @PutMapping("/campaigns/{id}")
    public ResponseEntity<RecruitmentCampaign> updateCampaign(@PathVariable Long id,
                                                              @RequestBody RecruitmentCampaign updated) {
        RecruitmentCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (updated.getTitle() != null) campaign.setTitle(updated.getTitle());
        if (updated.getDescription() != null) campaign.setDescription(updated.getDescription());
        if (updated.getStartDate() != null) campaign.setStartDate(updated.getStartDate());
        if (updated.getEndDate() != null) campaign.setEndDate(updated.getEndDate());
        if (updated.getMaxCandidates() != null) campaign.setMaxCandidates(updated.getMaxCandidates());

        return ResponseEntity.ok(campaignRepository.save(campaign));
    }

    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<String> deleteCampaign(@PathVariable Long id) {
        campaignRepository.deleteById(id);
        return ResponseEntity.ok("Campaign deleted successfully");
    }

    @PostMapping("/campaigns/{id}/questions")
    public ResponseEntity<CampaignQuestion> addQuestion(@PathVariable Long id,
                                                        @RequestBody CampaignQuestion question) {
        RecruitmentCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        question.setCampaign(campaign);
        return ResponseEntity.ok(questionRepository.save(question));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<CampaignQuestion> updateQuestion(@PathVariable Long id,
                                                           @RequestBody CampaignQuestion updated) {
        CampaignQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (updated.getLabel() != null) question.setLabel(updated.getLabel());
        if (updated.getType() != null) question.setType(updated.getType());
        if (updated.getOptions() != null) question.setOptions(updated.getOptions());
        question.setRequired(updated.isRequired());
        if (updated.getOrderIndex() != null) question.setOrderIndex(updated.getOrderIndex());

        return ResponseEntity.ok(questionRepository.save(question));
    }

    @DeleteMapping("/questions/{id}")
    @Transactional
    public ResponseEntity<String> deleteQuestion(@PathVariable Long id) {
        CampaignQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        answerRepository.deleteByQuestionId(id);
        questionRepository.delete(question);

        return ResponseEntity.ok("Question deleted successfully");
    }

    @PostMapping("/campaigns/{id}/apply")
    public ResponseEntity<?> apply(@PathVariable Long id,
                                   @RequestBody ApplicationSubmissionDto submission) {
        RecruitmentCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (applicationRepository.existsByRecruitmentCampaignIdAndCandidateEmail(id, submission.getCandidateEmail())) {
            return ResponseEntity.badRequest().body("You have already applied to this campaign");
        }

        Application application = new Application();
        application.setRecruitmentCampaign(campaign);
        application.setCandidateName(submission.getCandidateName());
        application.setCandidateEmail(submission.getCandidateEmail());
        application.setCandidatePhone(submission.getCandidatePhone());
        application.setStatus(ApplicationStatus.NEW);
        application.setSubmissionDate(LocalDate.now());
        applicationRepository.save(application);

        for (AnswerDto answerDto : submission.getAnswers()) {
            CampaignQuestion question = questionRepository.findById(answerDto.getQuestionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            ApplicationAnswer answer = new ApplicationAnswer();
            answer.setApplication(application);
            answer.setQuestion(question);
            answer.setAnswer(answerDto.getAnswer());
            answerRepository.save(answer);
        }

        return ResponseEntity.ok("Application submitted successfully");
    }

    @GetMapping("/campaigns/{id}/applications")
    public ResponseEntity<List<Application>> getApplications(@PathVariable Long id) {
        return ResponseEntity.ok(applicationRepository.findByRecruitmentCampaignId(id));
    }

    @PutMapping("/applications/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Long id,
                                               @RequestParam ApplicationStatus status) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        application.setStatus(status);
        applicationRepository.save(application);
        return ResponseEntity.ok("Status updated successfully");
    }

    @GetMapping("/campaigns/public/{publicLink}")
    public ResponseEntity<?> getCampaignByPublicLink(@PathVariable String publicLink) {
        RecruitmentCampaign campaign = campaignRepository.findByPublicLink(publicLink)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        return ResponseEntity.ok(campaign);
    }
}