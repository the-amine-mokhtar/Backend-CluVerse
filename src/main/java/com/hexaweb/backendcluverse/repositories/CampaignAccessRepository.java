package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.event.CampaignAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
public interface CampaignAccessRepository extends JpaRepository<CampaignAccess, Long> {

    @Modifying
    @Transactional
    @Query("delete from CampaignAccess c where c.campaign.id = :campaignId")
    void deleteAllByCampaignId(@Param("campaignId") Long campaignId);

    List<CampaignAccess> findByCampaign_Id(Long campaignId);

    Optional<CampaignAccess> findByCampaign_IdAndClub_Id(Long campaignId, Long clubId);
    
    List<CampaignAccess> findByClub_Id(Long clubId);
}