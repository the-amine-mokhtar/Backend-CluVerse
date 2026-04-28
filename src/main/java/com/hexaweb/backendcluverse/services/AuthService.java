package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.*;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.enumerations.RoleType;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ClubRepository clubRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthService(UserRepository userRepository, MembershipRepository membershipRepository,
                       ClubRepository clubRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.clubRepository = clubRepository;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse signup(SignupRequest request) {
        if (request.getClubId() == null) {
            throw new RuntimeException("club_id is required");
        }
        
        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new RuntimeException("Club not found"));

        if (userRepository.findFirstByEmailOrderByIdDesc(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already taken!");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        user = userRepository.save(user);

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setClub(club);
        membership.setRole(RoleType.MEMBER);
        membership.setJoinDate(LocalDate.now());
        membership.setActive(true);
        membershipRepository.save(membership);

        String token = jwtUtil.generateToken(user, club.getId(), membership.getRole().name(), user.getFirstName(), user.getLastName());
        return new AuthResponse(token, user.getEmail());
    }

    public List<MembershipDto> login(LoginRequest request) {
        User user = userRepository.findFirstByEmailOrderByIdDesc(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return user.getMemberships().stream().map(m -> {
            MembershipDto dto = new MembershipDto();
            dto.setId(m.getId());
            dto.setClubId(m.getClub().getId());
            dto.setClubName(m.getClub().getName());
            dto.setRole(m.getRole().name());
            dto.setJoinDate(m.getJoinDate());
            dto.setActive(m.isActive());
            return dto;
        }).collect(Collectors.toList());
    }

    public AuthResponse loginWithClub(LoginClubRequest request) {
        User user = userRepository.findFirstByEmailOrderByIdDesc(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        Membership activeMembership = user.getMemberships().stream()
                .filter(m -> m.getClub().getId().equals(request.getClubId()) && m.isActive())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User does not have an active membership in the selected club"));

        String token = jwtUtil.generateToken(user, activeMembership.getClub().getId(), activeMembership.getRole().name(), user.getFirstName(), user.getLastName());
        return new AuthResponse(token, user.getEmail());
    }

    @Transactional
    public AuthResponse loginWithIdentifier(MemberLoginRequest request) {
        // First check if this identifier belongs to a pending Club activation
        com.hexaweb.backendcluverse.entities.Club pendingClub = clubRepository.findByActivationCode(request.getConnectionIdentifier()).orElse(null);
        
        if (pendingClub != null) {
            // Check password against the temporary password
            if (pendingClub.getTemporaryPassword() != null && pendingClub.getTemporaryPassword().equals(request.getPassword())) {
                // Auto-verify! Create the president user
                User president = new User();
                president.setEmail(pendingClub.getEmail());
                president.setFirstName("President");
                president.setLastName(pendingClub.getName());
                president.setPassword(BCrypt.hashpw(pendingClub.getTemporaryPassword(), BCrypt.gensalt()));
                president.setConnectionIdentifier(pendingClub.getActivationCode());
                userRepository.save(president);

                Membership membership = new Membership();
                membership.setUser(president);
                membership.setClub(pendingClub);
                membership.setRole(RoleType.PRESIDENT);
                membership.setJoinDate(java.time.LocalDate.now());
                membership.setActive(true);
                membershipRepository.save(membership);

                pendingClub.setIsClubVerified(true);
                pendingClub.setActivationCode(null);
                pendingClub.setTemporaryPassword(null);
                clubRepository.save(pendingClub);
                
                // We don't return here, we let the normal flow below find the newly created user!
            }
        }

        User user = userRepository.findByConnectionIdentifier(request.getConnectionIdentifier())
                .orElseThrow(() -> new RuntimeException("Identifiant ou mot de passe invalide"));

        System.out.println("Memberships count: " + user.getMemberships().size());
        user.getMemberships().forEach(m -> System.out.println("Club: " + m.getClub().getName() + " | Active: " + m.isActive()));

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Identifiant ou mot de passe invalide");
        }

        Membership activeMembership = user.getMemberships().stream()
                .filter(m -> m.getClub().getName().equals(request.getClubName()) && m.isActive())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Aucun membership actif trouvé pour ce club"));

        String token = jwtUtil.generateToken(user, activeMembership.getClub().getId(), activeMembership.getRole().name(), user.getFirstName(), user.getLastName());
        return new AuthResponse(token, user.getConnectionIdentifier());
    }
}
