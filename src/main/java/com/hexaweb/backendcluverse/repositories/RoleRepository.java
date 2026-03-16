package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}

