package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.CandidateRequest;
import com.hexaweb.backendcluverse.entities.election.Candidate;
import com.hexaweb.backendcluverse.services.CandidateService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class CandidateController {

    private final CandidateService candidateService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<Candidate> getCandidates(
            @RequestParam(required = false) Long electionId,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return electionId != null ? candidateService.findByElectionId(electionId) : candidateService.findAll();
    }

    @GetMapping("/{id}")
    public Candidate getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return candidateService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Candidate submitCandidacy(
            @RequestBody CandidateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        return candidateService.submitCandidacy(request, userId);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        candidateService.deleteById(id);
    }

    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }
}
