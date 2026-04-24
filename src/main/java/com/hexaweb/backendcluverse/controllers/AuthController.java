package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.*;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.services.AuthService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request) {
        try {
            AuthResponse response = authService.signup(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            List<MembershipDto> clubs = authService.login(request);
            return ResponseEntity.ok(clubs);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new AuthResponse(null, e.getMessage()));
        }
    }

    @PostMapping("/login-club")
    public ResponseEntity<AuthResponse> loginWithClub(@RequestBody LoginClubRequest request) {
        try {
            AuthResponse response = authService.loginWithClub(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new AuthResponse(null, e.getMessage()));
        }
    }

    @PostMapping("/login-member")
    public ResponseEntity<AuthResponse> loginMember(@RequestBody MemberLoginRequest request) {
        try {
            AuthResponse response = authService.loginWithIdentifier(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new AuthResponse(null, e.getMessage()));
        }
    }

    /**
     * OAuth2 club selection – called when an OAuth2 user has multiple active memberships.
     * The frontend sends the temporary JWT (role=PENDING) + the chosen clubId.
     */
    @PostMapping("/oauth2-select-club")
    public ResponseEntity<AuthResponse> oauth2SelectClub(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody java.util.Map<String, Long> body) {
        try {
            String tempToken = jwtUtil.resolveBearerToken(authHeader);
            Long userId = jwtUtil.extractUserId(tempToken);
            Long clubId = body.get("clubId");

            if (clubId == null) {
                return ResponseEntity.badRequest().body(new AuthResponse(null, "clubId is required"));
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            com.hexaweb.backendcluverse.entities.Membership membership = user.getMemberships().stream()
                    .filter(m -> m.getClub().getId().equals(clubId) && m.isActive())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No active membership in selected club"));

            String token = jwtUtil.generateToken(user, clubId,
                    membership.getRole().name(), user.getFirstName(), user.getLastName());
            return ResponseEntity.ok(new AuthResponse(token, user.getEmail()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new AuthResponse(null, e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {

        String token = jwtUtil.resolveBearerToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        Long clubId = jwtUtil.extractClubId(token);
        String role = jwtUtil.extractRole(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newToken = jwtUtil.generateToken(user, clubId, role, user.getFirstName(), user.getLastName());
        return ResponseEntity.ok(new AuthResponse(newToken, user.getEmail()));
    }
}
