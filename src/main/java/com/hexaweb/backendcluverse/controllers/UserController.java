package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.UpdateProfileRequest;
import com.hexaweb.backendcluverse.dto.GoogleCalendarConnectRequest;
import com.hexaweb.backendcluverse.dto.GoogleCalendarConnectionResponse;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.services.CloudinaryService;
import com.hexaweb.backendcluverse.services.UserService;
import com.hexaweb.backendcluverse.services.competencies.GoogleCalendarService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private GoogleCalendarService googleCalendarService;


    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userService.save(user);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return userService.save(user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.deleteById(id);
    }

    @GetMapping("/me")
    public ResponseEntity<User> getMe(@RequestHeader("Authorization") String authHeader) {
        String token = jwtUtil.resolveBearerToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestHeader("Authorization") String authHeader,
                                      @RequestBody UpdateProfileRequest request) {
        String token = jwtUtil.resolveBearerToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                return ResponseEntity.badRequest().body("Current password is required to set a new password");
            }
            if (!BCrypt.checkpw(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Current password is incorrect");
            }
            user.setPassword(BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));
        }

        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/me/photo")
    public ResponseEntity<String> updatePhoto(@RequestHeader("Authorization") String authHeader,
                                              @RequestParam("file") MultipartFile file) throws IOException {
        String token = jwtUtil.resolveBearerToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String photoUrl = cloudinaryService.uploadLogo(file);
        user.setPhotoUrl(photoUrl);
        userRepository.save(user);

        return ResponseEntity.ok(photoUrl);
    }

        @GetMapping("/me/google-calendar")
        public ResponseEntity<GoogleCalendarConnectionResponse> googleCalendarStatus(@RequestHeader("Authorization") String authHeader) {
        String token = jwtUtil.resolveBearerToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean connected = user.getGoogleCalendarRefreshToken() != null && !user.getGoogleCalendarRefreshToken().isBlank();
        return ResponseEntity.ok(GoogleCalendarConnectionResponse.builder()
            .connected(connected)
            .message(connected ? "Google Calendar connected" : "Google Calendar not connected")
            .build());
        }

        @PostMapping("/me/google-calendar/connect")
        public ResponseEntity<GoogleCalendarConnectionResponse> connectGoogleCalendar(@RequestHeader("Authorization") String authHeader,
                                               @Valid @RequestBody GoogleCalendarConnectRequest request) {
        String token = jwtUtil.resolveBearerToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        googleCalendarService.validateRefreshToken(request.getRefreshToken());
        user.setGoogleCalendarRefreshToken(request.getRefreshToken().trim());
        userRepository.save(user);

        return ResponseEntity.ok(GoogleCalendarConnectionResponse.builder()
            .connected(true)
            .message("Google Calendar connected successfully")
            .build());
        }

        @DeleteMapping("/me/google-calendar/disconnect")
        public ResponseEntity<GoogleCalendarConnectionResponse> disconnectGoogleCalendar(@RequestHeader("Authorization") String authHeader) {
        String token = jwtUtil.resolveBearerToken(authHeader);
        Long userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        user.setGoogleCalendarRefreshToken(null);
        userRepository.save(user);

        return ResponseEntity.ok(GoogleCalendarConnectionResponse.builder()
            .connected(false)
            .message("Google Calendar disconnected")
            .build());
        }
}

