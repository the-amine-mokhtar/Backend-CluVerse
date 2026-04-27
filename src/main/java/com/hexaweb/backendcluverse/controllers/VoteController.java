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
import com.hexaweb.backendcluverse.dto.VoteTestResponse;
import com.hexaweb.backendcluverse.entities.election.Candidate;
import com.hexaweb.backendcluverse.entities.election.Election;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.CandidateRepository;
import com.hexaweb.backendcluverse.repositories.ElectionRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class VoteController {

    private final VoteService voteService;
    private final VoteRepository voteRepository;
    private final JwtUtil jwtUtil;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;

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

    @GetMapping("/my")
    public VoteDTO getMyVote(
            @RequestParam Long electionId,
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        List<Vote> votes = voteService.findByElectionId(electionId);
        Vote myVote = votes.stream()
                .filter(v -> v.getVoter() != null && v.getVoter().getId().equals(userId))
                .findFirst()
                .orElse(null);
        return myVote != null ? mapToDTO(myVote) : null;
    }

    @GetMapping("/my/all")
    public List<VoteDTO> getMyVotes(
            @RequestHeader("Authorization") String authHeader) {
        String token = resolveToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        return voteRepository.findByVoterId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/{electionId}/test")
    public VoteTestResponse simulateVotes(
            @PathVariable Long electionId,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);

        // Validate election exists
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Election not found"));

        // Get candidates for this election
        List<Candidate> candidates = candidateRepository.findByElectionId(electionId);
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No candidates found for this election");
        }

        // Get all users for voting
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No users found in the system");
        }

        // Create 20 votes at random times over 20 seconds
        List<Long> createdVoteIds = new ArrayList<>();
        Random random = new Random();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule 20 vote creations
        for (int i = 0; i < 20; i++) {
            // Random delay between 0 and 20 seconds
            long delaySeconds = random.nextLong(21);
            
            int voteIndex = i;
            scheduler.schedule(() -> {
                try {
                    Candidate candidate = candidates.get(random.nextInt(candidates.size()));
                    User voter = users.get(random.nextInt(users.size()));

                    Vote vote = new Vote();
                    vote.setCandidate(candidate);
                    vote.setVoter(voter);
                    vote.setElection(election);
                    vote.setPosition(candidate.getPosition());
                    vote.setTimestamp(LocalDateTime.now());
                    vote.setValid(true);
                    vote.setVoteWeight(1);

                    Vote savedVote = voteRepository.save(vote);
                    createdVoteIds.add(savedVote.getId());
                } catch (Exception e) {
                    System.err.println("Error creating vote " + voteIndex + ": " + e.getMessage());
                }
            }, delaySeconds, TimeUnit.SECONDS);
        }

        // Wait for all scheduled tasks to complete (25 seconds to be safe)
        try {
            scheduler.shutdown();
            boolean completed = scheduler.awaitTermination(25, TimeUnit.SECONDS);
            if (!completed) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }

        // Get the created vote count before deletion
        int createdCount = createdVoteIds.size();

        // Delete all created votes
        for (Long voteId : createdVoteIds) {
            try {
                voteRepository.deleteById(voteId);
            } catch (Exception e) {
                System.err.println("Error deleting vote " + voteId + ": " + e.getMessage());
            }
        }

        return new VoteTestResponse(
                "Test simulation completed",
                createdCount,
                createdVoteIds.size(),
                "All " + createdVoteIds.size() + " test votes have been created and deleted successfully"
        );
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
                vote.getVoter() != null ? vote.getVoter().getId() : null,
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
