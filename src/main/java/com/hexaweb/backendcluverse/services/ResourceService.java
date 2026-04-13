package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.ResourceRequest;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.logistics.Resource;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.ResourceRepository;
import org.springframework.stereotype.Service;

@Service
public class ResourceService extends EntityServiceImpl<Resource, Long> {
    
    private final ClubRepository clubRepository;

    public ResourceService(ResourceRepository repository, ClubRepository clubRepository) {
        super(repository);
        this.clubRepository = clubRepository;
    }

    public Resource createResource(ResourceRequest req) {
        Club club = clubRepository.findById(req.getClubId())
            .orElseThrow(() -> new RuntimeException("Club not found"));
        Resource r = new Resource();
        mapRequestToEntity(req, r);
        r.setClub(club);
        return save(r);
    }

    public Resource updateResource(Long id, ResourceRequest req) {
        Resource r = findById(id)
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        Club club = clubRepository.findById(req.getClubId())
            .orElseThrow(() -> new RuntimeException("Club not found"));
        mapRequestToEntity(req, r);
        r.setClub(club);
        return save(r);
    }

    private void mapRequestToEntity(ResourceRequest req, Resource r) {
        r.setName(req.getName());
        r.setDescription(req.getDescription());
        r.setUnitCost(req.getUnitCost());
        r.setStatus(req.getStatus());
        r.setImageUrl(req.getImageUrl());
        r.setQuantityTotal(req.getQuantityTotal());
        r.setAvailableQuantity(req.getAvailableQuantity());
        r.setLastUpdated(req.getLastUpdated());
        r.setLowStockThreshold(req.getLowStockThreshold());
        r.setNotes(req.getNotes());
    }
}
