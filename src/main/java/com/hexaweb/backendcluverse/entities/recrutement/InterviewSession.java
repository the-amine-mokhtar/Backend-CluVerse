package com.hexaweb.backendcluverse.entities.recrutement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "interview_session")
public class InterviewSession {

    @Id
    private String id;

    @Column(nullable = false)
    private Long positionId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false)
    private String status = "RUNNING";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}