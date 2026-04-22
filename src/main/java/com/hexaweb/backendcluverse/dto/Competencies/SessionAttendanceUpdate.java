package com.hexaweb.backendcluverse.dto.Competencies;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionAttendanceUpdate {

    @NotNull
    private Long userId;

    @NotNull
    private Boolean attended;
}
