package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsor;
import com.hexaweb.backendcluverse.services.SponsorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sponsors")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;

    @Value("${app.base-url}")
    private String frontendBaseUrl;

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
        return sponsorService.createPendingSponsor(sponsor);
    }

    @PutMapping("/{id}")
    public Sponsor update(@PathVariable Long id, @RequestBody Sponsor sponsor) {
        sponsor.setId(id);
        return sponsorService.save(sponsor);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestParam String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deletion reason is required");
        }
        sponsorService.deleteWithReason(id, reason);
    }

    @PostMapping("/{id}/logo")
    public Sponsor uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return sponsorService.uploadLogo(id, file);
    }

    @GetMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestParam String token) {
        try {
            Sponsor sponsor = sponsorService.confirmByToken(token);
            String result = sponsor.getStatus().name().toLowerCase();
            return redirectToFrontend(result);
        } catch (RuntimeException e) {
            return redirectToFrontend("invalid");
        }
    }

    @GetMapping("/deny")
    public ResponseEntity<Void> deny(@RequestParam String token) {
        try {
            Sponsor sponsor = sponsorService.denyByToken(token);
            String result = sponsor.getStatus().name().toLowerCase();
            return redirectToFrontend(result);
        } catch (RuntimeException e) {
            return redirectToFrontend("invalid");
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String result) {
        String target = frontendBaseUrl + "/sponsor-response?result=" + result;
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(target));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}

