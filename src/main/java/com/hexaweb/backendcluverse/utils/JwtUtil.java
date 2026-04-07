package com.hexaweb.backendcluverse.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.entities.User;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET = "hexaweb_7ell_el_beb_2026";
    private final long EXPIRATION_TIME = 864_000_000;

    public String generateToken(User user, Long clubId, String role, String firstName, String lastName) {
        return JWT.create()
                .withSubject(user.getId() != null ? user.getId().toString() : "")
                .withClaim("email", user.getEmail())
                .withClaim("clubid", clubId)
                .withClaim("role", role)
                .withClaim("isSuperAdmin", user.isSuperAdmin())
                .withClaim("firstName", firstName)
                .withClaim("lastName", lastName)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(SECRET.getBytes()));
    }
    
    public String validateTokenAndRetrieveSubject(String token) throws JWTVerificationException {
        return JWT.require(Algorithm.HMAC512(SECRET.getBytes()))
                .build()
                .verify(token)
                .getSubject();
    }

    public com.auth0.jwt.interfaces.DecodedJWT extractDecodedJWT(String token) throws JWTVerificationException {
        return JWT.require(Algorithm.HMAC512(SECRET.getBytes()))
                .build()
                .verify(token);
    }

    public Long extractUserId(String token) {
        String subject = extractDecodedJWT(token).getSubject();
        return Long.parseLong(subject);
    }

    public Long extractClubId(String token) {
        return extractDecodedJWT(token).getClaim("clubid").asLong();
    }

    public String extractRole(String token) {
        return extractDecodedJWT(token).getClaim("role").asString();
    }

    public boolean extractIsSuperAdmin(String token) {
        Boolean val = extractDecodedJWT(token).getClaim("isSuperAdmin").asBoolean();
        return val != null && val;
    }

    public String resolveBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.auth0.jwt.exceptions.JWTVerificationException("Missing or invalid Authorization header");
        }
        return authHeader.substring(7);
    }
}
