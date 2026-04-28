package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.SponsorshipStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveSponsorshipRequest {
    private SponsorshipStatus toStatus;
}
