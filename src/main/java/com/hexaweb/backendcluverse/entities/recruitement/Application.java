package com.hexaweb.backendcluverse.entities.recruitement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hexaweb.backendcluverse.enumerations.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;


    private String motivationLetter;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private LocalDateTime submissionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_campaign_id", nullable = false)
    private RecruitmentCampaign recruitmentCampaign;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ApplicationAnswer> answers = new ArrayList<>();

}

