package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.VoteRequest;
import com.hexaweb.backendcluverse.entities.election.Vote;
import com.hexaweb.backendcluverse.services.VoteService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class VoteController {

    private final VoteService voteService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<Vote> getVotes(
            @RequestParam(required = false) Long electionId,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return electionId != null ? voteService.findByElectionId(electionId) : voteService.findAll();
    }

    @GetMapping("/{id}")
    public Vote getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return voteService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Vote castVote(
            @RequestBody VoteRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long voterId = jwtUtil.extractUserId(token);
        return voteService.castVote(request, voterId);
    }

    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }
}
