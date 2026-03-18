package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

