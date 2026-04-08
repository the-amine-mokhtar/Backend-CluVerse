package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.CandidateRequest;
import com.hexaweb.backendcluverse.entities.election.Candidate;
import com.hexaweb.backendcluverse.enumerations.CandidateStatus;
import com.hexaweb.backendcluverse.entities.election.Election;
import com.hexaweb.backendcluverse.entities.election.Position;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.CandidateRepository;
import com.hexaweb.backendcluverse.repositories.ElectionRepository;
import com.hexaweb.backendcluverse.repositories.PositionRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CandidateService extends EntityServiceImpl<Candidate, Long> {

    private final CandidateRepository candidateRepository;
    private final ElectionRepository electionRepository;
    private final PositionRepository positionRepository;
    private final UserRepository userRepository;

    public CandidateService(CandidateRepository repository, ElectionRepository electionRepository,
                            PositionRepository positionRepository, UserRepository userRepository) {
        super(repository);
        this.candidateRepository = repository;
        this.electionRepository = electionRepository;
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
    }

    public List<Candidate> findByElectionId(Long electionId) {
        return candidateRepository.findByElectionId(electionId);
    }

    public Candidate submitCandidacy(CandidateRequest req, Long userId) {
        boolean alreadyApplied = candidateRepository.existsByUserIdAndElectionIdAndStatusIn(
                userId, req.getElectionId(),
                List.of(CandidateStatus.PENDING, CandidateStatus.APPROVED)
        );
        if (alreadyApplied) {
            throw new RuntimeException("You already have a pending or approved candidacy for this election");
        }

        Election election = electionRepository.findById(req.getElectionId())
                .orElseThrow(() -> new RuntimeException("Election not found with id: " + req.getElectionId()));

        Position position = positionRepository.findById(req.getPositionId())
                .orElseThrow(() -> new RuntimeException("Position not found with id: " + req.getPositionId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Candidate candidate = new Candidate();
        candidate.setElection(election);
        candidate.setPosition(position);
        candidate.setUser(user);
        candidate.setProgram(req.getProgram());
        candidate.setBio(req.getBio());
        candidate.setStatus(CandidateStatus.PENDING);
        candidate.setSubmissionDate(LocalDateTime.now());

        Candidate saved = candidateRepository.save(candidate);
        candidateRepository.flush();
        return saved;
    }

    public Candidate updateCandidacy(Long id, CandidateRequest req) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
        
        if (req.getElectionId() != null) {
            Election election = electionRepository.findById(req.getElectionId())
                    .orElseThrow(() -> new RuntimeException("Election not found"));
            candidate.setElection(election);
        }
        
        if (req.getPositionId() != null) {
            Position position = positionRepository.findById(req.getPositionId())
                    .orElseThrow(() -> new RuntimeException("Position not found"));
            candidate.setPosition(position);
        }
        
        if (req.getProgram() != null) {
            candidate.setProgram(req.getProgram());
        }
        if (req.getBio() != null) {
            candidate.setBio(req.getBio());
        }
        return candidateRepository.save(candidate);
    }
}
