package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.InviteMemberRequest;
import com.hexaweb.backendcluverse.dto.MemberProfileDto;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.services.CloudinaryService;
import com.hexaweb.backendcluverse.services.ClubService;
import com.hexaweb.backendcluverse.services.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.hexaweb.backendcluverse.dto.MembershipDto;
import com.hexaweb.backendcluverse.enumerations.RoleType;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final VerificationService verificationService;
    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;


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

    @PostMapping("/{id}/logo")
    public ResponseEntity<String> uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Club club = clubService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String logoUrl = cloudinaryService.uploadLogo(file);
        club.setLogoUrl(logoUrl);
        clubService.save(club);

        return ResponseEntity.ok(logoUrl);
    }

    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = clubService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/{clubId}/members")
    public ResponseEntity<List<MemberProfileDto>> getMembers(@PathVariable Long clubId) {
        List<Membership> memberships = membershipRepository.findByClubId(clubId);
        List<MemberProfileDto> members = memberships.stream().map(m -> {
            MemberProfileDto dto = new MemberProfileDto();
            dto.setUserId(m.getUser().getId());
            dto.setFirstName(m.getUser().getFirstName());
            dto.setLastName(m.getUser().getLastName());
            dto.setEmail(m.getUser().getEmail());
            dto.setRole(m.getRole().name());
            dto.setJoinDate(m.getJoinDate());
            dto.setActive(m.isActive());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }


    @PostMapping("/{clubId}/members/send-invite")
    public ResponseEntity<?> inviteMember(@PathVariable Long clubId,
                                          @RequestBody InviteMemberRequest request) {
        Club club = clubService.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (membershipRepository.existsByClubIdAndUserId(clubId,
                userRepository.findByEmail(request.getEmail())
                        .map(User::getId).orElse(-1L))) {
            return ResponseEntity.badRequest().body("User is already a member of this club");
        }

        verificationService.sendMemberInvitationEmail(request.getEmail(), club, request.getRole());
        return ResponseEntity.ok("Invitation sent to " + request.getEmail());
    }


    @DeleteMapping("/{clubId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long clubId,
                                          @PathVariable Long userId) {
        Membership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));

        membershipRepository.delete(membership);
        userRepository.deleteById(userId);

        return ResponseEntity.ok("Member removed successfully");
    }

    @PutMapping("/{clubId}/members/{userId}/deactivate")
    public ResponseEntity<String> deactivateMember(@PathVariable Long clubId,
                                                   @PathVariable Long userId) {
        Membership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        membership.setActive(false);
        membershipRepository.save(membership);
        return ResponseEntity.ok("Member deactivated successfully");
    }

    @PutMapping("/{clubId}/members/{userId}/activate")
    public ResponseEntity<String> activateMember(@PathVariable Long clubId,
                                                 @PathVariable Long userId) {
        Membership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        membership.setActive(true);
        membershipRepository.save(membership);
        return ResponseEntity.ok("Member activated successfully");
    }


}

