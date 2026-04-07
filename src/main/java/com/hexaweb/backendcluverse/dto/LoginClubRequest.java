package com.hexaweb.backendcluverse.dto;

import lombok.Data;

@Data
public class LoginClubRequest {
    private String email;
    private String password;
    private Long clubId;
}
