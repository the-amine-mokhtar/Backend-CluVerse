package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Election;
import com.hexaweb.backendcluverse.services.ElectionService;
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
@RequestMapping("/api/elections")
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionService electionService;

    @GetMapping
    public List<Election> getAll() {
        return electionService.findAll();
    }

    @GetMapping("/{id}")
    public Election getById(@PathVariable Long id) {
        return electionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Election create(@RequestBody Election election) {
        return electionService.save(election);
    }

    @PutMapping("/{id}")
    public Election update(@PathVariable Long id, @RequestBody Election election) {
        election.setId(id);
        return electionService.save(election);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        electionService.deleteById(id);
    }
}

