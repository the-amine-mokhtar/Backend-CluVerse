package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing OAuth2 user information after successful authentication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2UserRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String provider; // "google" or "github"
    private String providerId;
}
