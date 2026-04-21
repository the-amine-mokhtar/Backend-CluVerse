package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.CampaignRequest;
import com.hexaweb.backendcluverse.entities.event.Campaign;
import com.hexaweb.backendcluverse.services.CampaignService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class CampaignController {

    private final CampaignService campaignService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<Campaign> getAll(@RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return campaignService.getAllCampaigns();
    }

    @GetMapping("/{id}")
    public Campaign getById(@PathVariable Long id,
                            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return campaignService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    @PostMapping
    public Campaign create(@RequestBody CampaignRequest request,
                           @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return campaignService.createCampaign(request);
    }

    @PutMapping("/{id}")
    public Campaign update(@PathVariable Long id,
                           @RequestBody CampaignRequest request,
                           @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return campaignService.updateCampaign(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        campaignService.deleteCampaign(id);
    }

    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }
}