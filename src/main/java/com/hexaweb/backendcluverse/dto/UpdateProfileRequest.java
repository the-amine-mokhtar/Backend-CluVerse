package com.hexaweb.backendcluverse.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String currentPassword;
    private String newPassword;
}