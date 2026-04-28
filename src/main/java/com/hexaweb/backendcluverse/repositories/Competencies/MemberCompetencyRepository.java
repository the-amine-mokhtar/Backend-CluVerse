package com.hexaweb.backendcluverse.repositories.Competencies;

import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse;
import com.hexaweb.backendcluverse.entities.competencies.MemberCompetency;
import com.hexaweb.backendcluverse.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MemberCompetencyRepository extends JpaRepository<MemberCompetency, Long> {

        List<MemberCompetency> findByUserId(Long userId);

        Optional<MemberCompetency> findByUserIdAndSkillId(Long userId, Long skillId);

        @Query("""
                select mc from MemberCompetency mc
                where mc.skillId in (
                        select c.id from Competency c where c.clubId = :clubId
                )
        """)
        List<MemberCompetency> findByClubId(@Param("clubId") Long clubId);

        @Query("""
                select mc.skillId, avg(mc.currentLevel)
                from MemberCompetency mc
                where mc.skillId in (
                        select c.id from Competency c where c.clubId = :clubId
                )
                group by mc.skillId
        """)
        List<Object[]> findAvgLevelByCompetencyForClub(@Param("clubId") Long clubId);

        @Query("""
                select mc from MemberCompetency mc
                where mc.skillId in (
                        select c.id from Competency c where c.clubId = :clubId
                )
                and mc.currentLevel < mc.targetLevel
        """)
        List<MemberCompetency> findGapsForClub(@Param("clubId") Long clubId);

    @Query("select new com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse(" +
            "mc.id, mc.userId, mc.skillId, c.name, c.category, mc.currentLevel, mc.targetLevel, " +
            "mc.previousLevel, mc.endorsementCount, (mc.targetLevel - mc.currentLevel), mc.lastUpdatedBy, mc.lastUpdated" +
            ") from MemberCompetency mc, Competency c " +
            "where mc.skillId = c.id and mc.userId = :userId")
    List<MemberCompetencyResponse> findResponsesByUserId(@Param("userId") Long userId);

    // Batch query optimization: fetch MemberCompetency + User names in one query
    @Query("""
            select new com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse(
                mc.id, mc.userId, mc.skillId, c.name, c.category, 
                mc.currentLevel, mc.targetLevel, mc.previousLevel, mc.endorsementCount, 
                (mc.targetLevel - mc.currentLevel), mc.lastUpdatedBy, mc.lastUpdated
            )
            from MemberCompetency mc
            join Competency c on mc.skillId = c.id
            join User u on mc.userId = u.id
            where mc.skillId in (select c2.id from Competency c2 where c2.clubId = :clubId)
    """)
    List<MemberCompetencyResponse> findEnrichedResponsesByClubId(@Param("clubId") Long clubId);

    // Batch query: fetch MemberCompetency for multiple IDs
    @Query("select mc from MemberCompetency mc where mc.id in :ids")
    List<MemberCompetency> findByIdBatch(@Param("ids") Collection<Long> ids);

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

    @Query("select count(mc.id) from MemberCompetency mc, Competency c where mc.skillId = c.id and c.clubId = :clubId")
    long countByClubId(@Param("clubId") Long clubId);

    boolean existsByUserIdAndSkillId(Long userId, Long skillId);

    boolean existsByUserIdAndSkillIdAndIdNot(Long userId, Long skillId, Long id);
}
