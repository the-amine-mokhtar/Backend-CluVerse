package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.recrutement.CampaignQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignQuestionRepository extends JpaRepository<CampaignQuestion, Long> {
    List<CampaignQuestion> findByCampaignIdOrderByOrderIndex(Long campaignId);
}