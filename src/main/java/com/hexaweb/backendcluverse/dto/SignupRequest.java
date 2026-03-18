package com.hexaweb.backendcluverse.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private Long clubId;
}
