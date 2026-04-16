package com.hexaweb.backendcluverse.controllers.Competencies;

import com.hexaweb.backendcluverse.dto.Competencies.CompetencyRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyResponse;
import com.hexaweb.backendcluverse.services.competencies.CompetencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/competencies")
@RequiredArgsConstructor
public class CompetencyController {

    private final CompetencyService competencyService;

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
}
