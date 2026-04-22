package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Location findByName(String name);
}
