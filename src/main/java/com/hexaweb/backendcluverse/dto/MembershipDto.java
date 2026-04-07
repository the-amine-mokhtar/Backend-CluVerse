package com.hexaweb.backendcluverse.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MembershipDto {
    private Long id;
    private Long clubId;
    private String clubName;
    private String role;
    private LocalDate joinDate;
    private boolean active;
}
