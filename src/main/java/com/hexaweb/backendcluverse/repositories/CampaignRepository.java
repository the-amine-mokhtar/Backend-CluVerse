package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

}