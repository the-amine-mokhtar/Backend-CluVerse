package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.FacebookOAuthResponse;
import com.hexaweb.backendcluverse.dto.FacebookPublishRequest;
import com.hexaweb.backendcluverse.dto.FacebookPublishResponse;
import com.hexaweb.backendcluverse.services.FacebookService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/social/facebook")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class FacebookController {

    private final FacebookService facebookService;
    private final JwtUtil jwtUtil;

    @GetMapping("/oauth/url")
    public Map<String, String> getOAuthUrl(@RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return Map.of("url", facebookService.buildOAuthUrl());
    }

    @GetMapping("/oauth/callback")
    public FacebookOAuthResponse oauthCallback(
            @RequestParam String code) {
        try {
            return facebookService.exchangeCodeForPageToken(code);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/publish")
    public FacebookPublishResponse publish(
            @RequestBody FacebookPublishRequest request,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        try {
            return facebookService.publishPhoto(request);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }
}
