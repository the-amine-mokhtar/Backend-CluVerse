package com.hexaweb.backendcluverse.services.Recrutement;

import com.hexaweb.backendcluverse.entities.recrutement.RecruitmentCampaign;
import com.hexaweb.backendcluverse.repositories.RecruitmentCampaignRepository;
import com.hexaweb.backendcluverse.services.EntityServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RecruitmentCampaignService extends EntityServiceImpl<RecruitmentCampaign, Long> {
    public RecruitmentCampaignService(RecruitmentCampaignRepository repository) {
        super(repository);
    }
}

