package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsorship;
import com.hexaweb.backendcluverse.enumerations.SponsorshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SponsorshipRepository extends JpaRepository<Sponsorship, Long> {
	List<Sponsorship> findAllByOrderByUpdatedAtDescIdDesc();

	List<Sponsorship> findByClubIdOrderByUpdatedAtDescIdDesc(Long clubId);

	Optional<Sponsorship> findByIdAndClubId(Long id, Long clubId);

	Optional<Sponsorship> findByOutreachResponseToken(String outreachResponseToken);

	Optional<Sponsorship> findBySignedUploadToken(String signedUploadToken);

	Optional<Sponsorship> findByPaymentPageToken(String paymentPageToken);

	List<Sponsorship> findByStatusOrderByUpdatedAtDescIdDesc(SponsorshipStatus status);

	List<Sponsorship> findByClubIdAndStatusOrderByUpdatedAtDescIdDesc(Long clubId, SponsorshipStatus status);
}

