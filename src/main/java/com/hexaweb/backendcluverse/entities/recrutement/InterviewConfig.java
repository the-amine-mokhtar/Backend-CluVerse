package com.hexaweb.backendcluverse.entities.recrutement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "interview_configs")
public class InterviewConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties({"interviewConfigs"})
    @JoinColumn(name = "application_id")
    private Application application;

    private Integer duration;
    private String level;
    private String interviewType;

    @Column(columnDefinition = "TEXT")
    private String presidentNotes;

    @Column(unique = true)
    private String uniqueLink;

    private String status; // EN_ATTENTE, TERMINE

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.uniqueLink = java.util.UUID.randomUUID().toString();
        this.status = "EN_ATTENTE";
    }
}