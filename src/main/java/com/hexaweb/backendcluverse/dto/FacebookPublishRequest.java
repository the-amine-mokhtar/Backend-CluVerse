package com.hexaweb.backendcluverse.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacebookPublishRequest {
    private String message;
    private String imageBase64;
    private boolean privatePost = true;
}
