package com.hexaweb.backendcluverse.dto.Competencies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyBulkImportResponse {
    private Long clubId;
    private int totalRows;
    private int createdCount;
    private int skippedCount;
    private List<CompetencyResponse> createdCompetencies;
    private List<CompetencyBulkImportErrorResponse> errors;
}