package com.hexaweb.backendcluverse.entities.sponsoring;

import com.hexaweb.backendcluverse.enumerations.SponsorEmailDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "sponsor_email")
public class SponsorEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SponsorEmailDirection direction;

    @Column
    private Long inReplyToId;

    @Column
    private String fromAddress;

    @Column
    private String toAddress;

    @Column
    private String externalMessageId;

    @Column
    private String threadId;

    @Column
    private String inReplyToMessageId;

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean pinned = false;

    @OneToMany(mappedBy = "sponsorEmail", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<SponsorEmailAttachment> attachments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sponsor_id", nullable = false)
    private Sponsor sponsor;
}
