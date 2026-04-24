package com.hexaweb.backendcluverse.config.oauth2;

import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;

/**
 * Handles successful OAuth2 authentication.
 * - If the user already has memberships, uses their real role & club.
 * - If the user has multiple clubs, passes them so the frontend can let them choose.
 * - If the user is new / has no memberships, redirects with no club info.
 */
@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    @Autowired
    public OAuth2SuccessHandler(UserRepository userRepository, JwtUtil jwtUtil, HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository, ClubRepository clubRepository, MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.httpCookieOAuth2AuthorizationRequestRepository = httpCookieOAuth2AuthorizationRequestRepository;
        this.clubRepository = clubRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 authentication successful for user: {}", authentication.getName());
        
        // Clear authorization request cookies
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String provider = extractProvider(request);

        // Extract user information from OAuth2 response
        String email = oAuth2User.getAttribute("email");

        // GitHub may not provide email - use login as fallback
        if (email == null || email.isEmpty()) {
            String login = oAuth2User.getAttribute("login");
            if (login != null) {
                email = login + "@github.oauth";
            }
        }

        String firstName = extractFirstName(oAuth2User);
        String lastName = extractLastName(oAuth2User);
        String photoUrl = extractPhotoUrl(oAuth2User);

        log.debug("OAuth2 User Info - Email: {}, Provider: {}", email, provider);

        // Check if user exists
        User user = userRepository.findFirstByEmailOrderByIdDesc(email).orElse(null);

        if (user == null) {
            // Create new user (no membership yet)
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhotoUrl(photoUrl);
            user.setConnectionIdentifier(provider + "_" + oAuth2User.getName());
            user.setPassword(""); // OAuth2 users don't have password
            user.setSuperAdmin(false);

            user = userRepository.save(user);
            log.info("New OAuth2 user created with ID: {}", user.getId());
        } else {
            // Update existing user with latest photo if available
            boolean updated = false;
            if (photoUrl != null && !photoUrl.equals(user.getPhotoUrl())) {
                user.setPhotoUrl(photoUrl);
                updated = true;
            }
            String existingProvider = user.getConnectionIdentifier() != null
                    ? user.getConnectionIdentifier().split("_")[0] : "";
            if (!provider.equals(existingProvider)) {
                user.setConnectionIdentifier(provider + "_" + oAuth2User.getName());
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
                log.info("OAuth2 user updated: {}", user.getId());
            }
        }

        // AUTO-VERIFY: Check if this email belongs to a pending Club
        com.hexaweb.backendcluverse.entities.Club pendingClub = clubRepository.findFirstByEmailOrderByIdDesc(email).orElse(null);
        if (pendingClub != null && (pendingClub.getIsClubVerified() == null || !pendingClub.getIsClubVerified())) {
            
            // Make sure the user doesn't already have a membership to this club
            boolean alreadyMember = user.getMemberships().stream()
                    .anyMatch(m -> m.getClub().getId().equals(pendingClub.getId()));
            
            if (!alreadyMember) {
                Membership membership = new Membership();
                membership.setUser(user);
                membership.setClub(pendingClub);
                membership.setRole(com.hexaweb.backendcluverse.enumerations.RoleType.PRESIDENT);
                membership.setJoinDate(java.time.LocalDate.now());
                membership.setActive(true);
                membershipRepository.save(membership);
    
                pendingClub.setIsClubVerified(true);
                pendingClub.setActivationCode(null);
                pendingClub.setTemporaryPassword(null);
                clubRepository.save(pendingClub);
                
                user.getMemberships().add(membership);
                log.info("Auto-verified club {} and assigned PRESIDENT role to user {}", pendingClub.getName(), user.getId());
            }
        }
        log.info("OAuth2 login process complete for user ID: {}", user.getId());

        // ── Determine role & club from memberships ──────────────────────────────
        List<Membership> activeMemberships = user.getMemberships().stream()
                .filter(Membership::isActive)
                .collect(Collectors.toList());

        String redirectUrl;

        if (activeMemberships.size() == 1) {
            // ── Single club: generate full token with role & clubId ──────────────
            Membership m = activeMemberships.get(0);
            String role = m.getRole().name();
            Long clubId = m.getClub().getId();

            String token = jwtUtil.generateToken(user, clubId, role,
                    user.getFirstName(), user.getLastName());
            log.info("OAuth2 user has 1 active membership – role: {}, clubId: {}", role, clubId);

            redirectUrl = appBaseUrl + "/auth/oauth2-callback?token="
                    + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + "&email=" + enc(user.getEmail())
                    + "&firstName=" + enc(user.getFirstName())
                    + "&lastName=" + enc(user.getLastName())
                    + "&userId=" + user.getId();

        } else if (activeMemberships.size() > 1) {
            // ── Multiple clubs: send a temporary token + membership list ─────────
            //    The frontend will show a club selector, then call /api/auth/oauth2-select-club
            String tempToken = jwtUtil.generateToken(user, null, "PENDING",
                    user.getFirstName(), user.getLastName());
            log.info("OAuth2 user has {} active memberships – sending list", activeMemberships.size());

            // Build a compact memberships string: clubId:clubName:role, ...
            String membershipsParam = activeMemberships.stream()
                    .map(m -> m.getClub().getId() + ":" + m.getClub().getName() + ":" + m.getRole().name())
                    .collect(Collectors.joining(","));

            redirectUrl = appBaseUrl + "/auth/oauth2-callback?token="
                    + URLEncoder.encode(tempToken, StandardCharsets.UTF_8)
                    + "&email=" + enc(user.getEmail())
                    + "&firstName=" + enc(user.getFirstName())
                    + "&lastName=" + enc(user.getLastName())
                    + "&userId=" + user.getId()
                    + "&memberships=" + URLEncoder.encode(membershipsParam, StandardCharsets.UTF_8);

        } else {
            // ── No memberships: user is new or hasn't joined a club yet ──────────
            String token = jwtUtil.generateToken(user, null, "MEMBER",
                    user.getFirstName(), user.getLastName());
            log.info("OAuth2 user has no active memberships");

            redirectUrl = appBaseUrl + "/auth/oauth2-callback?token="
                    + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + "&email=" + enc(user.getEmail())
                    + "&firstName=" + enc(user.getFirstName())
                    + "&lastName=" + enc(user.getLastName())
                    + "&userId=" + user.getId()
                    + "&noClub=true";
        }

        response.sendRedirect(redirectUrl);
    }

    /** URL-encode helper (null-safe). */
    private String enc(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private String extractProvider(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri.contains("google")) {
            return "google";
        } else if (requestUri.contains("github")) {
            return "github";
        }
        return "unknown";
    }

    private String extractFirstName(OAuth2User oAuth2User) {
        String givenName = oAuth2User.getAttribute("given_name");
        if (givenName != null) return givenName;
        String name = oAuth2User.getAttribute("name");
        if (name != null) {
            String[] parts = name.split(" ");
            return parts[0];
        }
        return "User";
    }

    private String extractLastName(OAuth2User oAuth2User) {
        String familyName = oAuth2User.getAttribute("family_name");
        if (familyName != null) return familyName;
        String name = oAuth2User.getAttribute("name");
        if (name != null) {
            String[] parts = name.split(" ");
            if (parts.length > 1) return parts[parts.length - 1];
        }
        return "";
    }

    private String extractPhotoUrl(OAuth2User oAuth2User) {
        String picture = oAuth2User.getAttribute("picture");
        if (picture != null) return picture;
        String avatarUrl = oAuth2User.getAttribute("avatar_url");
        if (avatarUrl != null) return avatarUrl;
        return null;
    }
}
