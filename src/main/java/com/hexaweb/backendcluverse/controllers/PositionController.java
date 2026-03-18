package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.PositionRequest;
import com.hexaweb.backendcluverse.entities.Position;
import com.hexaweb.backendcluverse.services.PositionService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class PositionController {

    private final PositionService positionService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<Position> getByClubId(
            @RequestParam(required = false) Long clubId,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return clubId != null ? positionService.findByClubId(clubId) : positionService.findAll();
    }

    @GetMapping("/{id}")
    public Position getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return positionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Position create(
            @RequestBody PositionRequest request,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return positionService.createPosition(request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        positionService.deleteById(id);
    }

    private String resolveToken(String authHeader) {
        try {
            return jwtUtil.resolveBearerToken(authHeader);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }
}
