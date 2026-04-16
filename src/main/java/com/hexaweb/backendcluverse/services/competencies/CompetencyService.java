package com.hexaweb.backendcluverse.services.competencies;

import com.hexaweb.backendcluverse.dto.Competencies.CompetencyRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyResponse;
import com.hexaweb.backendcluverse.entities.competencies.Competency;
import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetencyService {

    private final CompetencyRepository competencyRepository;

    public List<CompetencyResponse> getByClub(Long clubId) {
        return competencyRepository.findResponsesByClubId(clubId);
    }

    public CompetencyResponse getById(Long id) {
        return competencyRepository.findResponseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competency not found"));
    }

    public CompetencyResponse create(CompetencyRequest request) {
        validateUniqueness(request.getName(), request.getClubId(), null);

        Competency competency = toEntity(request);
        Competency saved = competencyRepository.save(competency);
        log.info("Created competency id={} for clubId={}", saved.getId(), saved.getClubId());
        return toResponse(saved);
    }

    public CompetencyResponse update(Long id, CompetencyRequest request) {
        Competency existing = competencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competency not found"));

        validateUniqueness(request.getName(), request.getClubId(), id);

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setCategory(request.getCategory());
        existing.setClubId(request.getClubId());

        Competency saved = competencyRepository.save(existing);
        log.info("Updated competency id={}", saved.getId());
        return toResponse(saved);
    }

    public void delete(Long id) {
        if (!competencyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competency not found");
        }

        competencyRepository.deleteById(id);
        log.info("Deleted competency id={}", id);
    }

    private void validateUniqueness(String name, Long clubId, Long currentId) {
        boolean exists = currentId == null
                ? competencyRepository.existsByNameAndClubId(name, clubId)
                : competencyRepository.existsByNameAndClubIdAndIdNot(name, clubId, currentId);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Competency already exists for this club");
        }
    }

    private Competency toEntity(CompetencyRequest request) {
        Competency competency = new Competency();
        competency.setName(request.getName());
        competency.setDescription(request.getDescription());
        competency.setCategory(request.getCategory());
        competency.setClubId(request.getClubId());
        return competency;
    }

    private CompetencyResponse toResponse(Competency competency) {
        CompetencyResponse response = new CompetencyResponse();
        response.setId(competency.getId());
        response.setName(competency.getName());
        response.setDescription(competency.getDescription());
        response.setCategory(competency.getCategory());
        response.setClubId(competency.getClubId());
        return response;
    }
}
