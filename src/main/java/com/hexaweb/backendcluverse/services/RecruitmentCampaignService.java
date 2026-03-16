package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.RecruitmentCampaign;
import com.hexaweb.backendcluverse.repositories.RecruitmentCampaignRepository;
import org.springframework.stereotype.Service;

@Service
public class RecruitmentCampaignService extends EntityServiceImpl<RecruitmentCampaign, Long> {
    public RecruitmentCampaignService(RecruitmentCampaignRepository repository) {
        super(repository);
    }
}

