package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.AnswerDto;
import com.hexaweb.backendcluverse.dto.ApplicationSubmissionDto;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.Notification;
import com.hexaweb.backendcluverse.entities.recruitement.*;
import com.hexaweb.backendcluverse.repositories.*;
import com.hexaweb.backendcluverse.enumerations.ApplicationStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationRepository notificationRepository;

    @PostMapping("/campaigns")
    public ResponseEntity<?> createCampaign(@RequestBody RecruitmentCampaign campaign, @RequestParam Long clubId) {
        if (campaign.getEndDate() != null && campaign.getEndDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body("La date de fin doit être supérieure à la date de début");
        }
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        campaign.setClub(club);
        campaign.setPublicLink(UUID.randomUUID().toString());
        campaign.setActive(true);
        return ResponseEntity.ok(campaignRepository.save(campaign));
    }

    @PutMapping("/campaigns/{id}")
    public ResponseEntity<?> updateCampaign(@PathVariable Long id, @RequestBody RecruitmentCampaign updated) {
        RecruitmentCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (updated.getEndDate() != null && updated.getEndDate().isBefore(campaign.getStartDate())) {
            return ResponseEntity.badRequest().body("La date de fin doit être supérieure à la date de début");
        }

        campaign.setTitle(updated.getTitle());
        campaign.setDescription(updated.getDescription());
        campaign.setEndDate(updated.getEndDate());
        campaign.setMaxCandidates(updated.getMaxCandidates());

        if (updated.getEndDate() != null && LocalDate.now().isAfter(updated.getEndDate())) {
            campaign.setActive(false);
        } else {
            campaign.setActive(true);
        }

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

        // Vérification date expiration
        if (campaign.getEndDate() != null && LocalDate.now().isAfter(campaign.getEndDate())) {
            return ResponseEntity.badRequest().body("Cette campagne de recrutement est terminée");
        }

        // Vérification max candidats
        if (campaign.getMaxCandidates() != null) {
            long count = applicationRepository.countByRecruitmentCampaignId(id);
            if (count >= campaign.getMaxCandidates()) {
                return ResponseEntity.badRequest().body("Cette campagne a atteint le nombre maximum de candidats");
            }
        }

        // Vérification doublon
        if (applicationRepository.existsByRecruitmentCampaignIdAndCandidateEmail(id, submission.getCandidateEmail())) {
            return ResponseEntity.badRequest().body("Vous avez déjà postulé à cette campagne");
        }

        Application application = new Application();
        application.setRecruitmentCampaign(campaign);
        application.setCandidateName(submission.getCandidateName());
        application.setCandidateEmail(submission.getCandidateEmail());
        application.setCandidatePhone(submission.getCandidatePhone());
        application.setStatus(ApplicationStatus.NEW);
        application.setSubmissionDate(LocalDateTime.now());
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
        Notification notification = new Notification();
        notification.setClubId(campaign.getClub().getId());
        notification.setMessage("Nouvelle candidature de " + submission.getCandidateName() + " pour \"" + campaign.getTitle() + "\"");
        notification.setApplicationId(application.getId());
        notification.setCandidateName(submission.getCandidateName());
        notification.setCampaignTitle(campaign.getTitle());
        notificationRepository.save(notification);
        return ResponseEntity.ok("Application submitted successfully");
    }

    @GetMapping("/campaigns/{id}/applications")
    public ResponseEntity<List<Application>> getApplications(@PathVariable Long id) {
        return ResponseEntity.ok(applicationRepository.findByRecruitmentCampaignId(id));
    }

    @PutMapping("/applications/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Long id, @RequestParam ApplicationStatus status) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        application.setStatus(status);
        applicationRepository.save(application);

        // Envoi email
        sendStatusEmail(application, status);

        return ResponseEntity.ok("Status updated successfully");
    }

    private void sendStatusEmail(Application application, ApplicationStatus status) {
        String subject;
        String message;
        String clubName = application.getRecruitmentCampaign().getClub().getName();
        String campaignTitle = application.getRecruitmentCampaign().getTitle();

        switch (status) {
            case REVIEWING:
                subject = "Votre candidature est en cours d'examen";
                message = "Bonjour " + application.getCandidateName() + ",\n\n"
                        + "Votre candidature pour la campagne \"" + campaignTitle + "\" du club " + clubName
                        + " est actuellement en cours d'examen par notre équipe.\n\n"
                        + "Nous vous tiendrons informé(e) de la suite.";
                break;
            case INTERVIEW:
                subject = "Vous êtes sélectionné(e) pour un entretien";
                message = "Bonjour " + application.getCandidateName() + ",\n\n"
                        + "Félicitations ! Votre candidature pour la campagne \"" + campaignTitle + "\" du club " + clubName
                        + " a été retenue pour un entretien.\n\n"
                        + "Notre équipe vous contactera prochainement pour convenir d'une date.";
                break;
            case ACCEPTED:
                subject = "Votre candidature a été acceptée !";
                message = "Bonjour " + application.getCandidateName() + ",\n\n"
                        + "Nous avons le plaisir de vous informer que votre candidature pour la campagne \""
                        + campaignTitle + "\" du club " + clubName + " a été acceptée !\n\n"
                        + "Bienvenue parmi nous. Nous vous contacterons prochainement pour les prochaines étapes.";
                break;
            case REJECTED:
                subject = "Réponse à votre candidature";
                message = "Bonjour " + application.getCandidateName() + ",\n\n"
                        + "Nous vous remercions de l'intérêt que vous avez porté à la campagne \""
                        + campaignTitle + "\" du club " + clubName + ".\n\n"
                        + "Après examen de votre dossier, nous ne sommes malheureusement pas en mesure de donner "
                        + "une suite favorable à votre candidature.\n\n"
                        + "Nous vous souhaitons bonne chance dans vos recherches.";
                break;
            default:
                return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(application.getCandidateEmail());
        mail.setSubject(subject);
        mail.setText(message + "\n\nCordialement,\nL'équipe " + clubName);
        mailSender.send(mail);
    }

    @GetMapping("/campaigns/public/{publicLink}")
    public ResponseEntity<?> getCampaignByPublicLink(@PathVariable String publicLink) {
        RecruitmentCampaign campaign = campaignRepository.findByPublicLink(publicLink)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        return ResponseEntity.ok(campaign);
    }

    @GetMapping("/campaigns/{id}/stats")
    public ResponseEntity<?> getCampaignStats(@PathVariable Long id) {
        campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Application> applications = applicationRepository.findByRecruitmentCampaignId(id);
        long total = applications.size();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("NEW", applications.stream().filter(a -> a.getStatus() == ApplicationStatus.NEW).count());
        byStatus.put("REVIEWING", applications.stream().filter(a -> a.getStatus() == ApplicationStatus.REVIEWING).count());
        byStatus.put("INTERVIEW", applications.stream().filter(a -> a.getStatus() == ApplicationStatus.INTERVIEW).count());
        byStatus.put("ACCEPTED", applications.stream().filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED).count());
        byStatus.put("REJECTED", applications.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count());

        double conversionRate = total > 0
                ? Math.round((byStatus.get("ACCEPTED") * 100.0 / total) * 10.0) / 10.0
                : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalApplications", total);
        stats.put("byStatus", byStatus);
        stats.put("conversionRate", conversionRate);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/campaigns/{id}/export/csv")
    public ResponseEntity<byte[]> exportApplicationsCSV(@PathVariable Long id) {
        RecruitmentCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Application> applications = applicationRepository.findByRecruitmentCampaignId(id);
        List<CampaignQuestion> questions = questionRepository.findByCampaignIdOrderByOrderIndex(id);

        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Nom,Email,Téléphone,Statut,Date de soumission");
        for (CampaignQuestion question : questions) {
            csv.append(",").append(question.getLabel().replace(",", " "));
        }
        csv.append("\n");

        // Rows
        for (Application application : applications) {
            csv.append(application.getCandidateName()).append(",")
                    .append(application.getCandidateEmail()).append(",")
                    .append(application.getCandidatePhone() != null ? application.getCandidatePhone() : "").append(",")
                    .append(application.getStatus()).append(",")
                    .append(application.getSubmissionDate());

            // Réponses par question
            for (CampaignQuestion question : questions) {
                String answer = application.getAnswers().stream()
                        .filter(a -> a.getQuestion().getId().equals(question.getId()))
                        .map(ApplicationAnswer::getAnswer)
                        .findFirst()
                        .orElse("");
                csv.append(",").append(answer.replace(",", " ").replace("\n", " "));
            }
            csv.append("\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "candidatures-" + campaign.getTitle().replace(" ", "-") + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(bytes);
    }
}