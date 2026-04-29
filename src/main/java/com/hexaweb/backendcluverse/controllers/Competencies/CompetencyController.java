package com.hexaweb.backendcluverse.controllers.Competencies;

import com.hexaweb.backendcluverse.dto.Competencies.*;
import com.hexaweb.backendcluverse.services.competencies.CompetencyService;
import com.hexaweb.backendcluverse.services.competencies.MemberCompetencyService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/competencies")
@RequiredArgsConstructor
public class CompetencyController {

    private final CompetencyService competencyService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<CompetencyResponse>> getByClub(@RequestParam Long clubId) {
        return ResponseEntity.ok(competencyService.getByClub(clubId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetencyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(competencyService.getById(id));
    }

    @PostMapping
    public ResponseEntity<CompetencyResponse> create(@Valid @RequestBody CompetencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competencyService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetencyResponse> update(@PathVariable Long id, @Valid @RequestBody CompetencyRequest request) {
        return ResponseEntity.ok(competencyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        competencyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompetencyBulkImportResponse> bulkImport(@RequestParam Long clubId,
                                                                   @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competencyService.bulkImport(clubId, file));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<CompetencyResponse> cloneCompetency(@PathVariable Long id,
                                                              @Valid @RequestBody CompetencyCloneRequest request,
                                                              @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        requireSuperAdmin(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(competencyService.cloneToClub(id, request.getTargetClubId()));
    }

    @GetMapping("/stats")
    public ResponseEntity<CompetencyStatsResponse> getStats(@RequestParam Long clubId) {
        return ResponseEntity.ok(competencyService.getStats(clubId));
    }

    private void requireSuperAdmin(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Super admin access required");
        }

        boolean isSuperAdmin;
        try {
            isSuperAdmin = jwtUtil.extractIsSuperAdmin(jwtUtil.resolveBearerToken(authorization));
        } catch (Exception ex) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Super admin access required");
        }

        if (!isSuperAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Super admin access required");
        }
    }

}
