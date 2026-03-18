package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.AuthResponse;
import com.hexaweb.backendcluverse.dto.LoginClubRequest;
import com.hexaweb.backendcluverse.dto.LoginRequest;
import com.hexaweb.backendcluverse.dto.MembershipDto;
import com.hexaweb.backendcluverse.dto.SignupRequest;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.entities.RoleType;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.utils.JwtUtil;
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

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
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

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail());
    }

    public List<MembershipDto> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
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
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        boolean belongsToClub = user.getMemberships().stream()
                .anyMatch(m -> m.getClub().getId().equals(request.getClubId()) && m.isActive());

        if (!belongsToClub) {
             throw new RuntimeException("User does not have an active membership in the selected club");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail());
    }
}
