package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.enumerations.RoleType;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.services.VerificationService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private ClubRepository clubRepository;

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String code) {
        Club club = clubRepository.findByActivationCode(code)
                .orElseThrow(() -> new RuntimeException("Code invalide"));

        if (club.getActivationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Code expiré");
        }

        User president = new User();
        president.setEmail(club.getEmail());
        president.setFirstName("President");
        president.setLastName(club.getName());
        president.setPassword(BCrypt.hashpw(club.getTemporaryPassword(), BCrypt.gensalt()));
        userRepository.save(president);

        Membership membership = new Membership();
        membership.setUser(president);
        membership.setClub(club);
        membership.setRole(RoleType.PRESIDENT);
        membership.setJoinDate(LocalDate.now());
        membership.setActive(true);
        membershipRepository.save(membership);

        club.setIsClubVerified(true);
        club.setActivationCode(null);
        club.setTemporaryPassword(null);
        clubRepository.save(club);

        return ResponseEntity.ok(Map.of("message", "Compte activé", "email", president.getEmail()));
    }



    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;


}