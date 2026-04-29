package com.hexaweb.backendcluverse.dto.Competencies;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencySessionScheduleRequest {

    @NotNull
    private Long clubId;

    @NotNull
    private Long competencyId;

    @NotBlank
    private String title;

    @NotNull
    private Long coachUserId;

    @NotNull
    private LocalDateTime startsAt;

    private String meetLink;

    @NotEmpty
    private List<Long> participantUserIds;
}
