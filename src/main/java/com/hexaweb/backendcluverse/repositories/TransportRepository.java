package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.logistics.Transport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportRepository extends JpaRepository<Transport, Long> {
}
