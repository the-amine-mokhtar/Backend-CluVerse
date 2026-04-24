package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response after OAuth2 callback - contains JWT token and user info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2CallbackResponse {
    private String token;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String photoUrl;
}
