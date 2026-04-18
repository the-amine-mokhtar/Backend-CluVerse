package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.ElectionRequest;
import com.hexaweb.backendcluverse.dto.CandidateVoteResultDTO;
import com.hexaweb.backendcluverse.dto.ElectionCloseResponseDTO;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.election.Candidate;
import com.hexaweb.backendcluverse.entities.election.Election;
import com.hexaweb.backendcluverse.enumerations.ElectionStatus;
import com.hexaweb.backendcluverse.entities.election.Position;
import com.hexaweb.backendcluverse.entities.election.Vote;
import com.hexaweb.backendcluverse.repositories.CandidateRepository;
import com.hexaweb.backendcluverse.repositories.ElectionRepository;
import com.hexaweb.backendcluverse.repositories.PositionRepository;
import com.hexaweb.backendcluverse.repositories.VoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElectionService extends EntityServiceImpl<Election, Long> {

    private final ElectionRepository electionRepository;
    private final PositionRepository positionRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;

    public ElectionService(
            ElectionRepository repository,
            PositionRepository positionRepository,
            CandidateRepository candidateRepository,
            VoteRepository voteRepository
    ) {
        super(repository);
        this.electionRepository = repository;
        this.positionRepository = positionRepository;
        this.candidateRepository = candidateRepository;
        this.voteRepository = voteRepository;
    }

    public List<Election> findByClubId(Long clubId) {
        return electionRepository.findByPositionClubId(clubId);
    }

    public Election createElection(ElectionRequest req) {
        Position position = positionRepository.findById(req.getPositionId())
                .orElseThrow(() -> new RuntimeException("Position not found with id: " + req.getPositionId()));

        if (!position.getClub().getId().equals(req.getClubId())) {
            throw new RuntimeException("Position does not belong to the specified club");
        }

        boolean existsActive = electionRepository.existsByPositionIdAndStatusIn(
                req.getPositionId(),
                List.of(ElectionStatus.DRAFT, ElectionStatus.OPEN)
        );
        if (existsActive) {
            throw new RuntimeException("This position already has an active or draft election ongoing");
        }

        Election election = new Election();
        election.setTitle(req.getTitle());
        election.setDescription(req.getDescription());
        election.setStartDate(req.getStartDate());
        election.setEndDate(req.getEndDate());
        election.setStatus(req.getStatus());
        election.setPosition(position);

        Election saved = electionRepository.save(election);
        electionRepository.flush();
        return saved;
    }

    public Election updateElection(Long id, ElectionRequest req) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found with id: " + id));

        if (req.getTitle() != null) election.setTitle(req.getTitle());
        if (req.getDescription() != null) election.setDescription(req.getDescription());
        if (req.getStartDate() != null) election.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) election.setEndDate(req.getEndDate());
        if (req.getStatus() != null) election.setStatus(req.getStatus());

        Election saved = electionRepository.save(election);
        electionRepository.flush();
        return saved;
    }

    public ElectionCloseResponseDTO closeElection(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new RuntimeException("Election not found with id: " + electionId));

        List<Candidate> candidates = candidateRepository.findByElectionId(electionId);
        if (candidates.isEmpty()) {
            throw new RuntimeException("Cannot close election: no candidates found.");
        }

        List<Vote> votes = voteRepository.findByElectionId(electionId);
        Map<Long, Long> votesByCandidate = new HashMap<>();
        votes.forEach(vote -> {
            Long candidateId = vote.getCandidate() != null ? vote.getCandidate().getId() : null;
            if (candidateId != null) {
                votesByCandidate.put(candidateId, votesByCandidate.getOrDefault(candidateId, 0L) + 1);
            }
        });

        List<CandidateVoteResultDTO> rankedCandidates = candidates.stream()
                .map(candidate -> {
                    User user = candidate.getUser();
                    String firstName = user != null ? user.getFirstName() : "Unknown";
                    String lastName = user != null ? user.getLastName() : "";
                    return new CandidateVoteResultDTO(
                            candidate.getId(),
                            firstName,
                            lastName,
                            (firstName + " " + lastName).trim(),
                            votesByCandidate.getOrDefault(candidate.getId(), 0L)
                    );
                })
                .sorted(Comparator
                        .comparingLong(CandidateVoteResultDTO::getVotes).reversed()
                        .thenComparing(CandidateVoteResultDTO::getFullName))
                .toList();

        long topVotes = rankedCandidates.get(0).getVotes();
        long topCount = rankedCandidates.stream().filter(candidate -> candidate.getVotes() == topVotes).count();
        if (topCount > 1) {
            throw new RuntimeException("Election cannot be closed because it's a tie for first place.");
        }

        Candidate winnerCandidate = candidates.stream()
                .filter(candidate -> candidate.getId().equals(rankedCandidates.get(0).getCandidateId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Winner candidate was not found."));

        Position position = election.getPosition();
        position.setCurrentHolder(winnerCandidate.getUser());
        position.setHeldSince(LocalDate.now());
        positionRepository.save(position);

        election.setStatus(ElectionStatus.CLOSED);
        electionRepository.save(election);
        electionRepository.flush();

        String clubName = position.getClub() != null ? position.getClub().getName() : "";
        User winner = winnerCandidate.getUser();
        return new ElectionCloseResponseDTO(
                election.getId(),
                election.getTitle(),
                position.getName(),
                clubName,
                election.getStartDate(),
                LocalDate.now(),
                winnerCandidate.getId(),
                winner != null ? winner.getFirstName() : "",
                winner != null ? winner.getLastName() : "",
                rankedCandidates
        );
    }
}
