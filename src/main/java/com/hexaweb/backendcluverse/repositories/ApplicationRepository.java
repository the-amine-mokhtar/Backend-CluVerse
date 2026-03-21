package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.recruitement.Application;
import com.hexaweb.backendcluverse.enumerations.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByRecruitmentCampaignId(Long campaignId);
    List<Application> findByRecruitmentCampaignIdAndStatus(Long campaignId, ApplicationStatus status);
    boolean existsByRecruitmentCampaignIdAndCandidateEmail(Long campaignId, String email);
}

