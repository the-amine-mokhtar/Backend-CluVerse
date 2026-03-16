package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

