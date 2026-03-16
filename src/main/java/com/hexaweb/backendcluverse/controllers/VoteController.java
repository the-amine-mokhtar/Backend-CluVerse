package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Vote;
import com.hexaweb.backendcluverse.services.VoteService;
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
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @GetMapping
    public List<Vote> getAll() {
        return voteService.findAll();
    }

    @GetMapping("/{id}")
    public Vote getById(@PathVariable Long id) {
        return voteService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Vote create(@RequestBody Vote vote) {
        return voteService.save(vote);
    }

    @PutMapping("/{id}")
    public Vote update(@PathVariable Long id, @RequestBody Vote vote) {
        vote.setId(id);
        return voteService.save(vote);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        voteService.deleteById(id);
    }
}

