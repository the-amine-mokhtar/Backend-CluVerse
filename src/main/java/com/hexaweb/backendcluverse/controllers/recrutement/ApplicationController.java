package com.hexaweb.backendcluverse.controllers.recrutement;

import com.hexaweb.backendcluverse.entities.recrutement.InterviewConfig;
import com.hexaweb.backendcluverse.entities.recrutement.Application;
import com.hexaweb.backendcluverse.services.Recrutement.ApplicationService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.hexaweb.backendcluverse.dto.InterviewConfigRequest;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public List<Application> getAll() {
        return applicationService.findAll();
    }

    @GetMapping("/{id}")
    public Application getById(@PathVariable Long id) {
        return applicationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Application create(@RequestBody Application application) {
        return applicationService.save(application);
    }

    @PutMapping("/{id}")
    public Application update(@PathVariable Long id, @RequestBody Application application) {
        application.setId(id);
        return applicationService.save(application);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        applicationService.deleteById(id);
    }

    @PostMapping("/{id}/interview")
    public ResponseEntity<InterviewConfig> passToInterview(
            @PathVariable Long id,
            @RequestBody InterviewConfigRequest request) {
        return ResponseEntity.ok(applicationService.passToInterview(
                id,
                request.getDuration(),
                request.getLevel(),
                request.getInterviewType(),
                request.getPresidentNotes()
        ));
    }
}

