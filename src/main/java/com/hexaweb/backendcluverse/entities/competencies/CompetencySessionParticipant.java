package com.hexaweb.backendcluverse.entities.competencies;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "competency_session_participant",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_session_participant", columnNames = {"session_id", "user_id"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencySessionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private CompetencySession session;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column
    private Boolean attended;
}
