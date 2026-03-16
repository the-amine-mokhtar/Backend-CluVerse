package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorRepository extends JpaRepository<Sponsor, Long> {
}

