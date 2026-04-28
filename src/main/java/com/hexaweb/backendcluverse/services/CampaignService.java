package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.CampaignRequest;
import com.hexaweb.backendcluverse.entities.event.Campaign;
import com.hexaweb.backendcluverse.repositories.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service

public class CampaignService extends EntityServiceImpl<Campaign, Long> {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository repository) {
        super(repository);
        this.campaignRepository = repository;
    }

    // 🔍 GET ALL
    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    // ✅ CREATE
    public Campaign createCampaign(CampaignRequest req) {
        Campaign campaign = new Campaign();
        campaign.setTitle(req.getTitle());
        campaign.setDescription(req.getDescription());
        campaign.setStartDate(req.getStartDate());
        campaign.setEndDate(req.getEndDate());
        campaign.setBudget(req.getBudget());
        return campaignRepository.save(campaign);
    }

    // 🔄 UPDATE
    public Campaign updateCampaign(Long id, CampaignRequest req) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        if (req.getTitle() != null) campaign.setTitle(req.getTitle());
        if (req.getDescription() != null) campaign.setDescription(req.getDescription());
        if (req.getStartDate() != null) campaign.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) campaign.setEndDate(req.getEndDate());
        if (req.getBudget() > 0) campaign.setBudget(req.getBudget());

        return campaignRepository.save(campaign);
    }

    // ❌ DELETE
    public void deleteCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));
        campaignRepository.delete(campaign);
    }
}