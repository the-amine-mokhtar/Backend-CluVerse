package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.CampaignView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignViewRepository extends JpaRepository<CampaignView, Long> {
    void deleteAllByCampaignId(Long campaignId);
    /**
     * Check if a user has already viewed a campaign
     * @param campaignId the campaign ID
     * @param userId the user ID
     * @return true if the user has viewed this campaign before
     */
    boolean existsByCampaignIdAndUserId(Long campaignId, Long userId);

    /**
     * Get the view record for a specific user and campaign
     */
    Optional<CampaignView> findByCampaignIdAndUserId(Long campaignId, Long userId);
}
