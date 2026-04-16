package com.hexaweb.backendcluverse.repositories.Competencies;

import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse;
import com.hexaweb.backendcluverse.entities.competencies.MemberCompetency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberCompetencyRepository extends JpaRepository<MemberCompetency, Long> {

    @Query("select new com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse(" +
            "mc.id, mc.userId, mc.skillId, c.name, c.category, mc.currentLevel, mc.targetLevel, " +
            "mc.previousLevel, mc.endorsementCount, (mc.targetLevel - mc.currentLevel), mc.lastUpdatedBy, mc.lastUpdated" +
            ") from MemberCompetency mc, Competency c " +
            "where mc.skillId = c.id and mc.userId = :userId")
    List<MemberCompetencyResponse> findResponsesByUserId(@Param("userId") Long userId);

    @Query("select new com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse(" +
            "mc.id, mc.userId, mc.skillId, c.name, c.category, mc.currentLevel, mc.targetLevel, " +
            "mc.previousLevel, mc.endorsementCount, (mc.targetLevel - mc.currentLevel), mc.lastUpdatedBy, mc.lastUpdated" +
            ") from MemberCompetency mc, Competency c " +
            "where mc.skillId = c.id and c.clubId = :clubId")
    List<MemberCompetencyResponse> findResponsesByClubId(@Param("clubId") Long clubId);

    @Query("select new com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse(" +
            "mc.id, mc.userId, mc.skillId, c.name, c.category, mc.currentLevel, mc.targetLevel, " +
            "mc.previousLevel, mc.endorsementCount, (mc.targetLevel - mc.currentLevel), mc.lastUpdatedBy, mc.lastUpdated" +
            ") from MemberCompetency mc, Competency c " +
            "where mc.skillId = c.id and mc.id = :id")
    Optional<MemberCompetencyResponse> findResponseById(@Param("id") Long id);

    boolean existsByUserIdAndSkillId(Long userId, Long skillId);

    boolean existsByUserIdAndSkillIdAndIdNot(Long userId, Long skillId, Long id);
}
