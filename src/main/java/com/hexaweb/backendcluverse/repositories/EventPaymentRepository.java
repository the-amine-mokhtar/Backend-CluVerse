package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.EventPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventPaymentRepository extends JpaRepository<EventPayment, Long> {

}

