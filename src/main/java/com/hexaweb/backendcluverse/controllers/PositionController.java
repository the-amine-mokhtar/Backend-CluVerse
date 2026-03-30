package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.PositionRequest;
import com.hexaweb.backendcluverse.entities.election.Position;
import com.hexaweb.backendcluverse.services.PositionService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.hexaweb.backendcluverse.dto.PositionDTO;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class PositionController {

    private final PositionService positionService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<PositionDTO> getByClubId(
            @RequestParam(required = false) Long clubId,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        List<Position> positions = clubId != null ? positionService.findByClubId(clubId) : positionService.findAll();
        return positions.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PositionDTO getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        Position position = positionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mapToDTO(position);
    }

    @PostMapping
    public PositionDTO create(
            @RequestBody PositionRequest request,
            @RequestHeader("Authorization") String authHeader) {
        resolveToken(authHeader);
        return mapToDTO(positionService.createPosition(request));
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

    private PositionDTO mapToDTO(Position position) {
        if (position == null) return null;
        return new PositionDTO(
                position.getId(),
                position.getName(),
                position.getDescription(),
                position.getTermLength(),
                position.getMaxCandidates(),
                position.isElectable(),
                position.isAutoRenew(),
                position.getCurrentHolder() != null ? position.getCurrentHolder().getFirstName() + " " + position.getCurrentHolder().getLastName() : null
        );
    }
}
