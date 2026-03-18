package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.ElectionRequest;
import com.hexaweb.backendcluverse.entities.Election;
import com.hexaweb.backendcluverse.entities.Position;
import com.hexaweb.backendcluverse.repositories.ElectionRepository;
import com.hexaweb.backendcluverse.repositories.PositionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElectionService extends EntityServiceImpl<Election, Long> {

    private final ElectionRepository electionRepository;
    private final PositionRepository positionRepository;

    public ElectionService(ElectionRepository repository, PositionRepository positionRepository) {
        super(repository);
        this.electionRepository = repository;
        this.positionRepository = positionRepository;
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

        Election election = new Election();
        election.setTitle(req.getTitle());
        election.setDescription(req.getDescription());
        election.setStartDate(req.getStartDate());
        election.setEndDate(req.getEndDate());
        election.setStatus(req.getStatus());
        election.setPosition(position);

        return electionRepository.save(election);
    }

    public Election updateElection(Long id, ElectionRequest req) {
        Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found with id: " + id));

        if (req.getTitle() != null) election.setTitle(req.getTitle());
        if (req.getDescription() != null) election.setDescription(req.getDescription());
        if (req.getStartDate() != null) election.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) election.setEndDate(req.getEndDate());
        if (req.getStatus() != null) election.setStatus(req.getStatus());

        return electionRepository.save(election);
    }
}
