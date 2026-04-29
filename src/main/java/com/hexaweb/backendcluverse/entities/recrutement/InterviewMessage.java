package com.hexaweb.backendcluverse.entities.recrutement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "interview_messages")
public class InterviewMessage {

    @Id
    private String id;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "role", length = 50)
    private String role;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "timestamp_ms")
    private Long timestampMs;

    @Column(name = "elapsed_sec")
    private Integer elapsedSec;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }
}