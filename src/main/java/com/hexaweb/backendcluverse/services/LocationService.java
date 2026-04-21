package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.event.Location;
import com.hexaweb.backendcluverse.repositories.LocationRepository;
import org.springframework.stereotype.Service;

@Service
public class LocationService extends EntityServiceImpl<Location, Long> {
    public LocationService(LocationRepository repository) {
        super(repository);
    }
}
