package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.logistics.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}

