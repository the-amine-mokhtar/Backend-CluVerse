package com.hexaweb.backendcluverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacebookOAuthResponse {
    private String accessToken;
    private String pageAccessToken;
    private String pageId;
    private String pageName;
}
