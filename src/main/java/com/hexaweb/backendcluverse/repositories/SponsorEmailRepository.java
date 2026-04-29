package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.sponsoring.SponsorEmail;
import com.hexaweb.backendcluverse.enumerations.SponsorEmailDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SponsorEmailRepository extends JpaRepository<SponsorEmail, Long> {
    List<SponsorEmail> findBySponsorIdAndClubIdOrderBySentAtDesc(Long sponsorId, Long clubId);

    List<SponsorEmail> findBySponsorIdAndClubIdAndPinnedTrueOrderBySentAtDesc(Long sponsorId, Long clubId);

    Optional<SponsorEmail> findByIdAndSponsorIdAndClubId(Long id, Long sponsorId, Long clubId);

    boolean existsByExternalMessageIdAndClubId(String externalMessageId, Long clubId);

        boolean existsBySponsorIdAndClubIdAndDirectionAndSentAtAfter(Long sponsorId, Long clubId, SponsorEmailDirection direction, LocalDateTime sentAt);

            boolean existsBySponsorIdAndClubIdAndDirectionAndSentAtAfterAndSubjectContainingIgnoreCase(
                Long sponsorId,
                Long clubId,
                SponsorEmailDirection direction,
                LocalDateTime sentAt,
                String subjectFragment
            );

        @Query("""
            select count(se) > 0
            from SponsorEmail se
            where se.sponsor.id = :sponsorId
                            and se.club.id = :clubId
              and se.direction = com.hexaweb.backendcluverse.enumerations.SponsorEmailDirection.INBOUND
              and se.sentAt >= :afterTime
              and lower(coalesce(se.fromAddress, '')) like lower(concat('%', :emailFlag, '%'))
            """)
        boolean existsInboundBySponsorAfterWithEmailFlag(
            @Param("sponsorId") Long sponsorId,
                        @Param("clubId") Long clubId,
            @Param("afterTime") LocalDateTime afterTime,
            @Param("emailFlag") String emailFlag
        );
}
