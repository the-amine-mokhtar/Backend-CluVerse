package com.hexaweb.backendcluverse.entities.election;

import jakarta.persistence.*;

@Entity
@Table(name = "interview_report")
public class InterviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Column(nullable = false)
    private String memberName;

    @Column(nullable = false)
    private String positionTitle;

    @Column(nullable = false)
    private Integer globalScore;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(nullable = false)
    private Integer technicalScore;

    @Column(nullable = false)
    private Integer softSkillsScore;

    @Column(nullable = false)
    private Integer motivationScore;

    @Column(nullable = false)
    private String recommendation;

    // getters + setters
}