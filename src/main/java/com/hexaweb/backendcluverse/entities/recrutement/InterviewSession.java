package com.hexaweb.backendcluverse.entities.recrutement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    private String id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "role")
    private String role;

    @Column(name = "level")
    private String level;

    @Column(name = "interview_type")
    private String interviewType;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "status", length = 50)
    private String status = "running";

    @Column(name = "recruiter_impression", length = 50)
    private String recruiterImpression;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }
}