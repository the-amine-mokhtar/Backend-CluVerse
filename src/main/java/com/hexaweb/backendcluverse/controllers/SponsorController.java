package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsor;
import com.hexaweb.backendcluverse.services.SponsorService;
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
@RequestMapping("/api/sponsors")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;

    @GetMapping
    public List<Sponsor> getAll() {
        return sponsorService.findAll();
    }

    @GetMapping("/{id}")
    public Sponsor getById(@PathVariable Long id) {
        return sponsorService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Sponsor create(@RequestBody Sponsor sponsor) {
        return sponsorService.save(sponsor);
    }

    @PutMapping("/{id}")
    public Sponsor update(@PathVariable Long id, @RequestBody Sponsor sponsor) {
        sponsor.setId(id);
        return sponsorService.save(sponsor);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        sponsorService.deleteById(id);
    }
}

