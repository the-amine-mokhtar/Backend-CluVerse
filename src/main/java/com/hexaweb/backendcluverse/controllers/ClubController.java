package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.services.ClubService;
import com.hexaweb.backendcluverse.services.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.hexaweb.backendcluverse.dto.MembershipDto;
import com.hexaweb.backendcluverse.enumerations.RoleType;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final VerificationService verificationService;


    @GetMapping
    public List<Club> getAll() {
        return clubService.findAll();
    }

    @GetMapping("/{id}")
    public Club getById(@PathVariable Long id) {
        return clubService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Club create(@RequestBody Club club) {
        Club saved = clubService.save(club);
        verificationService.sendActivationEmail(saved);
        return saved;
    }

    @PutMapping("/{id}")
    public Club update(@PathVariable Long id, @RequestBody Club club) {
        club.setId(id);
        return clubService.save(club);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        clubService.deleteById(id);
    }

    @GetMapping("/names")
    public List<String> getAllClubsNames() {
        return clubService.findAll()
                .stream()  
                .map(Club::getName)  
                .collect(Collectors.toList());
    }

    @PostMapping("/{clubId}/members/{userId}")
    public MembershipDto addMember(@PathVariable Long clubId, 
                                   @PathVariable Long userId, 
                                   @RequestParam(required = false, defaultValue = "MEMBER") RoleType role) {
        return clubService.addMember(clubId, userId, role);
    }
}

