package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.CandidateRequest;
import com.hexaweb.backendcluverse.entities.election.Candidate;
import com.hexaweb.backendcluverse.services.CandidateService;
import com.hexaweb.backendcluverse.repositories.VoteRepository;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.hexaweb.backendcluverse.dto.CandidateDTO;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class CandidateController {

    private final CandidateService candidateService;
    private final VoteRepository voteRepository;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<CandidateDTO> getCandidates(
            @RequestParam(required = false) Long electionId,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        List<Candidate> candidates = electionId != null ? candidateService.findByElectionId(electionId) : candidateService.findAll();
        return candidates.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public CandidateDTO getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        Candidate candidate = candidateService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapToDTO(candidate);
    }

    @PostMapping
    public CandidateDTO submitCandidacy(
            @RequestBody CandidateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        return mapToDTO(candidateService.submitCandidacy(request, userId));
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        candidateService.deleteById(id);
    }

    @PutMapping("/{id}")
    public CandidateDTO update(
            @PathVariable Long id,
            @RequestBody CandidateRequest request,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return mapToDTO(candidateService.updateCandidacy(id, request));
    }

    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }

    private CandidateDTO mapToDTO(Candidate candidate) {
        if (candidate == null) return null;
        
        long voteCount = voteRepository.countByCandidateId(candidate.getId());
        long totalElectionVotes = (candidate.getElection() != null) ? voteRepository.countByElectionId(candidate.getElection().getId()) : 0;
        double percentage = (totalElectionVotes > 0) ? (double) voteCount / totalElectionVotes * 100 : 0.0;

        return new CandidateDTO(
                candidate.getId(),
                candidate.getProgram(),
                candidate.getStatus(),
                candidate.getSubmissionDate(),
                candidate.getWithdrawalDate(),
                candidate.getAiCritique(),
                candidate.getBio(),
                candidate.getUser() != null ? candidate.getUser().getFirstName() + " " + candidate.getUser().getLastName() : null,
                candidate.getUser() != null ? candidate.getUser().getEmail() : null,
                candidate.getElection() != null ? candidate.getElection().getId() : null,
                candidate.getPosition() != null ? candidate.getPosition().getId() : null,
                voteCount,
                totalElectionVotes,
                percentage
        );
    }
}
