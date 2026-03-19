package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsorship;
import com.hexaweb.backendcluverse.services.SponsorshipService;
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
@RequestMapping("/api/sponsorships")
@RequiredArgsConstructor
public class SponsorshipController {

    private final SponsorshipService sponsorshipService;

    @GetMapping
    public List<Sponsorship> getAll() {
        return sponsorshipService.findAll();
    }

    @GetMapping("/{id}")
    public Sponsorship getById(@PathVariable Long id) {
        return sponsorshipService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Sponsorship create(@RequestBody Sponsorship sponsorship) {
        return sponsorshipService.save(sponsorship);
    }

    @PutMapping("/{id}")
    public Sponsorship update(@PathVariable Long id, @RequestBody Sponsorship sponsorship) {
        sponsorship.setId(id);
        return sponsorshipService.save(sponsorship);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        sponsorshipService.deleteById(id);
    }
}

