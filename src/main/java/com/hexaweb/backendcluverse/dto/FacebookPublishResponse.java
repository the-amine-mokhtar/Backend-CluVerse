package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacebookPublishResponse {
    private String id;
    private String postId;
    private boolean published;
    private String message;
}
