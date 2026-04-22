package com.hexaweb.backendcluverse.dto.Competencies;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencySessionCloseRequest {

    private String reportSummary;

    private Boolean autoGenerateReport;

    private String speechSessionId;

    @Valid
    private List<SessionAttendanceUpdate> attendance;
}
