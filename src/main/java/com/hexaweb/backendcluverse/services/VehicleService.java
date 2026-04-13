package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.logistics.Vehicle;
import com.hexaweb.backendcluverse.repositories.VehicleRepository;
import org.springframework.stereotype.Service;

@Service
public class VehicleService extends EntityServiceImpl<Vehicle, Long> {
    public VehicleService(VehicleRepository repository) {
        super(repository);
    }
}
