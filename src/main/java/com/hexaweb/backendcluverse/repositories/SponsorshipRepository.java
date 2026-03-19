package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsorship;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorshipRepository extends JpaRepository<Sponsorship, Long> {
}

