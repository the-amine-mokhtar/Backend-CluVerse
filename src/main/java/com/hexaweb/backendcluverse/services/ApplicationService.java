package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.recrutement.InterviewConfig;
import com.hexaweb.backendcluverse.entities.recrutement.Application;
import com.hexaweb.backendcluverse.enumerations.ApplicationStatus;
import com.hexaweb.backendcluverse.repositories.ApplicationRepository;
import com.hexaweb.backendcluverse.repositories.InterviewConfigRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService extends EntityServiceImpl<Application, Long> {

    private final ApplicationRepository applicationRepository;
    private final InterviewConfigRepository interviewConfigRepository;
    private final JavaMailSender mailSender;

    public ApplicationService(ApplicationRepository applicationRepository,
                              InterviewConfigRepository interviewConfigRepository,
                              JavaMailSender mailSender,
                              JdbcTemplate jdbcTemplate) {
        super(applicationRepository);
        this.applicationRepository = applicationRepository;
        this.interviewConfigRepository = interviewConfigRepository;
        this.mailSender = mailSender;
    }

    public InterviewConfig passToInterview(Long applicationId, Integer duration, String level, String interviewType, String presidentNotes) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        application.setStatus(ApplicationStatus.INTERVIEW);
        applicationRepository.save(application);

        InterviewConfig config = new InterviewConfig();
        config.setApplication(application);
        config.setDuration(duration);
        config.setLevel(level);
        config.setInterviewType(interviewType);
        config.setPresidentNotes(presidentNotes);
        InterviewConfig saved = interviewConfigRepository.save(config);

        sendInterviewEmail(application, saved.getUniqueLink());

        return saved;
    }

    private void sendInterviewEmail(Application application, String uniqueLink) {
        String interviewUrl = "http://localhost:4200/interview/" + uniqueLink;
        String subject = "Invitation à votre entretien - "
                + application.getRecruitmentCampaign().getClub().getName();
        String body = "Bonjour " + application.getCandidateName() + ",\n\n"
                + "Félicitations ! Vous êtes sélectionné(e) pour passer à la phase d'entretien pour rejoindre le club "
                + application.getRecruitmentCampaign().getClub().getName() + ".\n\n"
                + "Vous pouvez passer votre entretien en cliquant sur le lien suivant :\n"
                + interviewUrl + "\n\n"
                + "Attention : cet entretien ne peut être passé qu'une seule fois.\n\n"
                + "Bonne chance !";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(application.getCandidateEmail());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
