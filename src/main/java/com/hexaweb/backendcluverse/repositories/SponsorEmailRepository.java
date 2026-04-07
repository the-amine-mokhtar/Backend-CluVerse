package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.sponsoring.SponsorEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SponsorEmailRepository extends JpaRepository<SponsorEmail, Long> {
    List<SponsorEmail> findBySponsorIdOrderBySentAtDesc(Long sponsorId);

    List<SponsorEmail> findBySponsorIdAndPinnedTrueOrderBySentAtDesc(Long sponsorId);

    Optional<SponsorEmail> findByIdAndSponsorId(Long id, Long sponsorId);

    boolean existsByExternalMessageId(String externalMessageId);
}
