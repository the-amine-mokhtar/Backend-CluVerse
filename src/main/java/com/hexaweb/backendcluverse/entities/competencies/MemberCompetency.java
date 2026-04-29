package com.hexaweb.backendcluverse.entities.competencies;

import com.hexaweb.backendcluverse.enumerations.UpdateSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_competency",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_competency_user_skill", columnNames = {"user_id", "skill_id"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCompetency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(nullable = false)
    private Integer currentLevel;

    @Column(nullable = false)
    private Integer targetLevel;

    @Column(nullable = false)
    private Integer previousLevel;

    @Column(nullable = false)
    private Integer endorsementCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UpdateSource lastUpdatedBy;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
}
