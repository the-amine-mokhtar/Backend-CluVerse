package com.hexaweb.backendcluverse.repositories.Competencies;

import com.hexaweb.backendcluverse.dto.Competencies.CompetencyResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyStatsCategoryResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyStatsCompetencyResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyStatsWeakCompetencyResponse;
import com.hexaweb.backendcluverse.entities.competencies.Competency;
import com.hexaweb.backendcluverse.enumerations.CompetencyType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    // Batch query optimization: fetch competencies by IDs
    @Query("select c from Competency c where c.id in :ids")
    List<Competency> findByIdBatch(@Param("ids") Collection<Long> ids);

    long countByClubId(Long clubId);

    @Query("select new com.hexaweb.backendcluverse.dto.Competencies.CompetencyStatsCompetencyResponse(" +
            "c.id, c.name, count(mc.id), coalesce(avg(mc.currentLevel), 0), coalesce(avg(mc.targetLevel), 0), coalesce(avg(mc.targetLevel - mc.currentLevel), 0)" +
            ") from Competency c left join MemberCompetency mc on mc.skillId = c.id " +
            "where c.clubId = :clubId " +
            "group by c.id, c.name " +
            "order by count(mc.id) desc, c.name asc")
    List<CompetencyStatsCompetencyResponse> findCompetencyMemberCountsByClubId(@Param("clubId") Long clubId);

    @Query("select new com.hexaweb.backendcluverse.dto.Competencies.CompetencyStatsCategoryResponse(" +
            "c.category, count(distinct c.id), count(mc.id), coalesce(avg(mc.currentLevel), 0), coalesce(avg(mc.targetLevel), 0), coalesce(avg(mc.targetLevel - mc.currentLevel), 0)" +
            ") from Competency c left join MemberCompetency mc on mc.skillId = c.id " +
            "where c.clubId = :clubId " +
            "group by c.category " +
            "order by c.category")
    List<CompetencyStatsCategoryResponse> findCategoryStatsByClubId(@Param("clubId") Long clubId);

    @Query("select new com.hexaweb.backendcluverse.dto.Competencies.CompetencyStatsWeakCompetencyResponse(" +
            "c.id, c.name, c.category, count(mc.id), coalesce(avg(mc.currentLevel), 0), coalesce(avg(mc.targetLevel), 0), coalesce(avg(mc.targetLevel - mc.currentLevel), 0)" +
            ") from Competency c left join MemberCompetency mc on mc.skillId = c.id " +
            "where c.clubId = :clubId " +
            "group by c.id, c.name, c.category " +
            "order by coalesce(avg(mc.targetLevel - mc.currentLevel), 0) desc, c.name asc")
    List<CompetencyStatsWeakCompetencyResponse> findWeakestCompetenciesByClubId(@Param("clubId") Long clubId, Pageable pageable);

    List<Competency> findByClubIdAndCategory(Long clubId, CompetencyType category);

    boolean existsByNameAndClubId(String name, Long clubId);

    boolean existsByNameAndClubIdAndIdNot(String name, Long clubId, Long id);

    Optional<Competency> findByNameAndClubId(String name, Long clubId);
}
