package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.PositionRequest;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.election.Position;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.PositionRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class PositionService extends EntityServiceImpl<Position, Long> {

    private final PositionRepository positionRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    public PositionService(PositionRepository repository, ClubRepository clubRepository, UserRepository userRepository) {
        super(repository);
        this.positionRepository = repository;
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
    }

    public List<Position> findByClubId(Long clubId) {
        return positionRepository.findByClubId(clubId);
    }

    public Position createPosition(PositionRequest req) {
        Club club = clubRepository.findById(req.getClubId())
                .orElseThrow(() -> new RuntimeException("Club not found with id: " + req.getClubId()));

        Position position = new Position();
        position.setName(req.getName());
        position.setDescription(req.getDescription());
        position.setTermLength(req.getTermLength());
        position.setMaxCandidates(req.getMaxCandidates());
        position.setIsElectable(req.getElectable());
        position.setIsAutoRenew(req.getAutoRenew());
        position.setClub(club);

        if (req.getCurrentHolderId() != null) {
            User holder = userRepository.findById(req.getCurrentHolderId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + req.getCurrentHolderId()));
            position.setCurrentHolder(holder);
        }

        return positionRepository.save(position);
    }

    public Position updatePosition(Long id, PositionRequest req) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Position not found"));

        if (req.getName() != null) position.setName(req.getName());
        if (req.getDescription() != null) position.setDescription(req.getDescription());

        position.setTermLength(req.getTermLength());
        position.setMaxCandidates(req.getMaxCandidates());

        position.setIsElectable(req.getElectable());
        position.setIsAutoRenew(req.getAutoRenew());

        if (req.getCurrentHolderId() != null) {
            User holder = userRepository.findById(req.getCurrentHolderId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + req.getCurrentHolderId()));
            Long currentHolderId = position.getCurrentHolder() != null
                    ? position.getCurrentHolder().getId()
                    : null;

            if (!Objects.equals(req.getCurrentHolderId(), currentHolderId)) {
                position.setHeldSince(LocalDate.now());
            }
            position.setCurrentHolder(holder);
        } else {
            position.setCurrentHolder(null);
        }

        return positionRepository.save(position);
    }

    public Position updateHeldSince(Long id, LocalDate heldSince) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Position not found"));
        position.setHeldSince(heldSince);
        return positionRepository.save(position);
    }
}
