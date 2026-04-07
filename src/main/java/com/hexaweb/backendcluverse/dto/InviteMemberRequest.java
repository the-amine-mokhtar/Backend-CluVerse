package com.hexaweb.backendcluverse.dto;

import lombok.Data;

@Data
public class InviteMemberRequest {
    private String email;
    private String role;
}