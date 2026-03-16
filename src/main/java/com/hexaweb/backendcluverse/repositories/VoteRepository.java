package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {
}

