package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.recrutement.RecruitmentCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecruitmentCampaignRepository extends JpaRepository<RecruitmentCampaign, Long> {
    List<RecruitmentCampaign> findByClubId(Long clubId);
    Optional<RecruitmentCampaign> findByPublicLink(String publicLink);

}

