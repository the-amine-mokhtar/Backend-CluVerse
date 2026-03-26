package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByClubId(Long clubId);
    Optional<Membership> findByClubIdAndUserId(Long clubId, Long userId);
    boolean existsByClubIdAndUserId(Long clubId, Long userId);
}

