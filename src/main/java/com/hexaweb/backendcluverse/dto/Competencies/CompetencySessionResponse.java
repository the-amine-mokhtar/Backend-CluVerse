package com.hexaweb.backendcluverse.dto.Competencies;

import com.hexaweb.backendcluverse.enumerations.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencySessionResponse {
    private Long id;
    private Long clubId;
    private Long competencyId;
    private String competencyName;
    private String title;
    private Long coachUserId;
    private String coachName;
    private String meetLink;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private SessionStatus status;
    private Integer participantCount;
    private String reportSummary;
    private String cancellationReason;
    private Boolean attended;
    private List<CompetencySessionParticipantResponse> participants;
}
