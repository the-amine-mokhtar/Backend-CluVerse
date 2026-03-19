package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.recruitement.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
}

