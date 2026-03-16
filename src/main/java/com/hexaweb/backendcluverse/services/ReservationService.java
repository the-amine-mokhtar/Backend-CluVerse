package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Reservation;
import com.hexaweb.backendcluverse.repositories.ReservationRepository;
import org.springframework.stereotype.Service;

@Service
public class ReservationService extends EntityServiceImpl<Reservation, Long> {
    public ReservationService(ReservationRepository repository) {
        super(repository);
    }
}

