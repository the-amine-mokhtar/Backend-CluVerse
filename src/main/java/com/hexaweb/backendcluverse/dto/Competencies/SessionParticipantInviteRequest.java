package com.hexaweb.backendcluverse.dto.Competencies;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionParticipantInviteRequest {

    @NotEmpty
    private List<Long> participantUserIds;
}
