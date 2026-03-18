package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.ElectionRequest;
import com.hexaweb.backendcluverse.entities.Election;
import com.hexaweb.backendcluverse.services.ElectionService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/api/elections")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ElectionController {

    private final ElectionService electionService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<Election> getElections(
            @RequestParam(required = false) Long clubId,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return clubId != null ? electionService.findByClubId(clubId) : electionService.findAll();
    }

    @GetMapping("/{id}")
    public Election getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return electionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Election create(
            @RequestBody ElectionRequest request,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return electionService.createElection(request);
    }

    @PutMapping("/{id}")
    public Election update(
            @PathVariable Long id,
            @RequestBody ElectionRequest request,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return electionService.updateElection(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        electionService.deleteById(id);
    }

    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }
}
