package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.*;
import com.hexaweb.backendcluverse.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

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
}
