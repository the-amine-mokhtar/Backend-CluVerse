package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorRepository extends JpaRepository<Sponsor, Long> {
}

