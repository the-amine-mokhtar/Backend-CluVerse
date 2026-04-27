package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.VoteRequest;
import com.hexaweb.backendcluverse.entities.election.Candidate;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.election.Vote;
import com.hexaweb.backendcluverse.repositories.CandidateRepository;
import com.hexaweb.backendcluverse.repositories.VoteRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoteService extends EntityServiceImpl<Vote, Long> {

    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;

    public VoteService(VoteRepository repository, CandidateRepository candidateRepository,
                       UserRepository userRepository) {
        super(repository);
        this.voteRepository = repository;
        this.candidateRepository = candidateRepository;
        this.userRepository = userRepository;
    }

    public List<Vote> findByElectionId(Long electionId) {
        return voteRepository.findByElectionId(electionId);
    }

    public Vote castVote(VoteRequest req, Long voterId) {
        boolean alreadyVoted = voteRepository.existsByVoterIdAndElectionId(voterId, req.getElectionId());
        if (alreadyVoted) {
            throw new RuntimeException("You have already voted in this election");
        }

        Candidate candidate = candidateRepository.findById(req.getCandidateId())
                .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + req.getCandidateId()));

        if (!candidate.getElection().getId().equals(req.getElectionId())) {
            throw new RuntimeException("Candidate does not belong to the specified election");
        }

        User voter = userRepository.findById(voterId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + voterId));

        Vote vote = new Vote();
        vote.setCandidate(candidate);
        vote.setVoter(voter);
        vote.setElection(candidate.getElection());
        vote.setPosition(candidate.getPosition());
        vote.setTimestamp(LocalDateTime.now());
        vote.setValid(true);
        vote.setVoteWeight(1);

        Vote saved = voteRepository.save(vote);
        voteRepository.flush();
        return saved;
    }

    public Vote updateVote(Long id, VoteRequest req) {
        Vote vote = voteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vote not found"));
        
        if (req.getCandidateId() != null) {
            Candidate candidate = candidateRepository.findById(req.getCandidateId())
                    .orElseThrow(() -> new RuntimeException("Candidate not found"));
            vote.setCandidate(candidate);
            vote.setElection(candidate.getElection());
            vote.setPosition(candidate.getPosition());
        }
        return voteRepository.save(vote);
    }
}
