package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.enumerations.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByConnectionIdentifier(String connectionIdentifier);
    
    /**
     * Find users with a specific role in a specific club
     * Uses the Membership entity to get the role
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.memberships m
        WHERE m.role = :role
          AND m.club.id = :clubId
          AND m.active = true
        """)
    List<User> findByClubIdAndRole(
            @Param("clubId") Long clubId,
            @Param("role")   RoleType role
    );
    @Query("SELECT u FROM User u JOIN u.memberships m WHERE m.club.id = :clubId AND m.role = :role")
    List<User> findByClubIdAndRole(@Param("clubId") Long clubId, @Param("role") String role);
}

