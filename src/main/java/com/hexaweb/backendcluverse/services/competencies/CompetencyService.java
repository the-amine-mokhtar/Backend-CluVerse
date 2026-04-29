package com.hexaweb.backendcluverse.services.competencies;

import com.hexaweb.backendcluverse.dto.Competencies.CompetencyBulkImportErrorResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyBulkImportResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyStatsResponse;
import com.hexaweb.backendcluverse.entities.competencies.Competency;
import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencyRepository;
import com.hexaweb.backendcluverse.repositories.Competencies.MemberCompetencyRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetencyService {

    private final CompetencyRepository competencyRepository;
    private final MemberCompetencyRepository memberCompetencyRepository;

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

    public CompetencyResponse cloneToClub(Long id, Long targetClubId) {
        Competency source = competencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competency not found"));

        if (targetClubId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target club is required");
        }

        if (source.getClubId().equals(targetClubId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Competency already belongs to this club");
        }

        validateUniqueness(source.getName(), targetClubId, null);

        Competency clone = new Competency();
        clone.setName(source.getName());
        clone.setDescription(source.getDescription());
        clone.setCategory(source.getCategory());
        clone.setClubId(targetClubId);

        Competency saved = competencyRepository.save(clone);
        log.info("Cloned competency id={} to clubId={}", id, targetClubId);
        return toResponse(saved);
    }

    public CompetencyBulkImportResponse bulkImport(Long clubId, MultipartFile file) {
        if (clubId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Club id is required");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is required");
        }

        List<CompetencyResponse> created = new ArrayList<>();
        List<CompetencyBulkImportErrorResponse> errors = new ArrayList<>();
        Set<String> seenNames = new LinkedHashSet<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreSurroundingSpaces(true)
                     .build()
                     .parse(reader)) {

            List<Competency> pending = new ArrayList<>();
            int totalRows = 0;

            for (CSVRecord record : parser) {
                totalRows += 1;
                int rowNumber = (int) record.getRecordNumber() + 1;
                String name = getColumn(record, "name");
                String description = getColumn(record, "description");
                String categoryValue = getColumn(record, "category");

                if (name.isBlank()) {
                    errors.add(new CompetencyBulkImportErrorResponse(rowNumber, name, "Name is required"));
                    continue;
                }

                String normalizedName = name.toLowerCase(Locale.ROOT);
                if (!seenNames.add(normalizedName)) {
                    errors.add(new CompetencyBulkImportErrorResponse(rowNumber, name, "Duplicate competency in CSV"));
                    continue;
                }

                CompetencyType category;
                try {
                    category = CompetencyType.valueOf(categoryValue.toUpperCase(Locale.ROOT));
                } catch (Exception ex) {
                    errors.add(new CompetencyBulkImportErrorResponse(rowNumber, name, "Invalid category: " + categoryValue));
                    continue;
                }

                if (competencyRepository.existsByNameAndClubId(name, clubId)) {
                    errors.add(new CompetencyBulkImportErrorResponse(rowNumber, name, "Competency already exists for this club"));
                    continue;
                }

                Competency competency = new Competency();
                competency.setName(name);
                competency.setDescription(description.isBlank() ? null : description);
                competency.setCategory(category);
                competency.setClubId(clubId);
                pending.add(competency);
            }

            List<Competency> saved = competencyRepository.saveAll(pending);
            created.addAll(saved.stream().map(this::toResponse).toList());

            log.info("Bulk imported {} competencies for clubId={}", created.size(), clubId);
            return new CompetencyBulkImportResponse(
                    clubId,
                    totalRows,
                    created.size(),
                    errors.size(),
                    created,
                    errors
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read CSV file", ex);
        }
    }

    public CompetencyStatsResponse getStats(Long clubId) {
        if (clubId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Club id is required");
        }

        Pageable topFive = PageRequest.of(0, 5);

        return new CompetencyStatsResponse(
                clubId,
                competencyRepository.countByClubId(clubId),
                memberCompetencyRepository.countByClubId(clubId),
                competencyRepository.findCompetencyMemberCountsByClubId(clubId),
                competencyRepository.findCategoryStatsByClubId(clubId),
                competencyRepository.findWeakestCompetenciesByClubId(clubId, topFive)
        );
    }

    private String getColumn(CSVRecord record, String name) {
        if (!record.isMapped(name)) {
            return "";
        }
        String value = record.get(name);
        return value == null ? "" : value.trim();
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
