package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Candidate;
import com.hexaweb.backendcluverse.services.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateService candidateService;

    @GetMapping
    public List<Candidate> getAll() {
        return candidateService.findAll();
    }

    @GetMapping("/{id}")
    public Candidate getById(@PathVariable Long id) {
        return candidateService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Candidate create(@RequestBody Candidate candidate) {
        return candidateService.save(candidate);
    }

    @PutMapping("/{id}")
    public Candidate update(@PathVariable Long id, @RequestBody Candidate candidate) {
        candidate.setId(id);
        return candidateService.save(candidate);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        candidateService.deleteById(id);
    }
}

