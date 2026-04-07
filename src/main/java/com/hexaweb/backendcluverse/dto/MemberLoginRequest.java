package com.hexaweb.backendcluverse.dto;

import lombok.Data;

@Data
public class MemberLoginRequest {
    private String connectionIdentifier;
    private String password;
    private String clubName;
}