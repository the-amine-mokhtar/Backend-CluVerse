package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SponsorRepository extends JpaRepository<Sponsor, Long> {
	Optional<Sponsor> findByConfirmationToken(String confirmationToken);

	Optional<Sponsor> findByContactEmailIgnoreCase(String contactEmail);

	List<Sponsor> findByClubIdOrderByIdDesc(Long clubId);

	Optional<Sponsor> findByIdAndClubId(Long id, Long clubId);

	Optional<Sponsor> findByContactEmailIgnoreCaseAndClubId(String contactEmail, Long clubId);
}

