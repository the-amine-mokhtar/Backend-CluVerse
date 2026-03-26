package com.hexaweb.backendcluverse.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Long clubId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private Long applicationId;

    @Column
    private String candidateName;

    @Column
    private String campaignTitle;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}