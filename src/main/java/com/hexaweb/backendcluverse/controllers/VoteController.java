package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.VoteRequest;
import com.hexaweb.backendcluverse.entities.election.Vote;
import com.hexaweb.backendcluverse.services.VoteService;
import com.hexaweb.backendcluverse.repositories.VoteRepository;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.hexaweb.backendcluverse.dto.VoteDTO;
import com.hexaweb.backendcluverse.dto.CandidateDTO;
import com.hexaweb.backendcluverse.entities.election.Candidate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class VoteController {

    private final VoteService voteService;
    private final VoteRepository voteRepository;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<VoteDTO> getVotes(
            @RequestParam(required = false) Long electionId,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        List<Vote> votes = electionId != null ? voteService.findByElectionId(electionId) : voteService.findAll();
        return votes.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public VoteDTO getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        Vote vote = voteService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapToDTO(vote);
    }

    @PostMapping
    public VoteDTO castVote(
            @RequestBody VoteRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long voterId = jwtUtil.extractUserId(token);
        return mapToDTO(voteService.castVote(request, voterId));
    }

    @PutMapping("/{id}")
    public VoteDTO update(
            @PathVariable Long id,
            @RequestBody VoteRequest request,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return mapToDTO(voteService.updateVote(id, request));
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        voteService.deleteById(id);
    }

    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }

    private VoteDTO mapToDTO(Vote vote) {
        if (vote == null)
            return null;
        return new VoteDTO(
                vote.getId(),
                vote.getTimestamp(),
                vote.isValid(),
                vote.getVoteWeight(),
                vote.getVoter() != null ? vote.getVoter().getFirstName() + " " + vote.getVoter().getLastName() : null,
                vote.getPosition() != null ? vote.getPosition().getName() : null,
                mapToCandidateDTO(vote.getCandidate()),
                vote.getElection() != null ? vote.getElection().getId() : null,
                vote.getPosition() != null ? vote.getPosition().getId() : null,
                vote.getElection() != null ? vote.getElection().getTitle() : null);
    }

    private CandidateDTO mapToCandidateDTO(Candidate candidate) {
        if (candidate == null)
            return null;

        long voteCount = voteRepository.countByCandidateId(candidate.getId());
        long totalElectionVotes = (candidate.getElection() != null)
                ? voteRepository.countByElectionId(candidate.getElection().getId())
                : 0;
        double percentage = (totalElectionVotes > 0) ? (double) voteCount / totalElectionVotes * 100 : 0.0;

        return new CandidateDTO(
                candidate.getId(),
                candidate.getProgram(),
                candidate.getStatus(),
                candidate.getSubmissionDate(),
                candidate.getWithdrawalDate(),
                candidate.getAiCritique(),
                candidate.getBio(),
                candidate.getUser() != null
                        ? candidate.getUser().getFirstName() + " " + candidate.getUser().getLastName()
                        : null,
                candidate.getUser() != null ? candidate.getUser().getEmail() : null,
                candidate.getElection() != null ? candidate.getElection().getId() : null,
                candidate.getPosition() != null ? candidate.getPosition().getId() : null,
                voteCount,
                totalElectionVotes,
                percentage);
    }
}
