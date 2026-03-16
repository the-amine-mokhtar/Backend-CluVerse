package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.RecruitmentCampaign;
import com.hexaweb.backendcluverse.services.RecruitmentCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/recruitment-campaigns")
@RequiredArgsConstructor
public class RecruitmentCampaignController {

    private final RecruitmentCampaignService recruitmentCampaignService;

    @GetMapping
    public List<RecruitmentCampaign> getAll() {
        return recruitmentCampaignService.findAll();
    }

    @GetMapping("/{id}")
    public RecruitmentCampaign getById(@PathVariable Long id) {
        return recruitmentCampaignService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public RecruitmentCampaign create(@RequestBody RecruitmentCampaign recruitmentCampaign) {
        return recruitmentCampaignService.save(recruitmentCampaign);
    }

    @PutMapping("/{id}")
    public RecruitmentCampaign update(@PathVariable Long id, @RequestBody RecruitmentCampaign recruitmentCampaign) {
        recruitmentCampaign.setId(id);
        return recruitmentCampaignService.save(recruitmentCampaign);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        recruitmentCampaignService.deleteById(id);
    }
}

