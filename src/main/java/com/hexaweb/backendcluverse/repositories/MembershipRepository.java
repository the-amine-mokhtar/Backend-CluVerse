package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
}

