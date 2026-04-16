package com.hexaweb.backendcluverse.repositories.Competencies;

import com.hexaweb.backendcluverse.dto.Competencies.CompetencyResponse;
import com.hexaweb.backendcluverse.entities.competencies.Competency;
import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompetencyRepository extends JpaRepository<Competency, Long> {

    @Query("select new com.hexaweb.backendcluverse.dto.Competencies.CompetencyResponse(" +
            "c.id, c.name, c.description, c.category, c.clubId" +
            ") from Competency c where c.clubId = :clubId")
    List<CompetencyResponse> findResponsesByClubId(@Param("clubId") Long clubId);

    @Query("select new com.hexaweb.backendcluverse.dto.Competencies.CompetencyResponse(" +
            "c.id, c.name, c.description, c.category, c.clubId" +
            ") from Competency c where c.id = :id")
    Optional<CompetencyResponse> findResponseById(@Param("id") Long id);

    List<Competency> findByClubIdAndCategory(Long clubId, CompetencyType category);

    boolean existsByNameAndClubId(String name, Long clubId);

    boolean existsByNameAndClubIdAndIdNot(String name, Long clubId, Long id);
}
