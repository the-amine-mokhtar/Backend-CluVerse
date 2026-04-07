package com.hexaweb.backendcluverse.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MemberProfileDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private LocalDate joinDate;
    private boolean active;
}