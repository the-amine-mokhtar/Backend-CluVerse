package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.FacebookOAuthResponse;
import com.hexaweb.backendcluverse.dto.FacebookPublishRequest;
import com.hexaweb.backendcluverse.dto.FacebookPublishResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class FacebookService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://graph.facebook.com")
            .build();

    @Value("${meta.facebook.app-id:}")
    private String appId;

    @Value("${meta.facebook.app-secret:}")
    private String appSecret;

    @Value("${meta.facebook.redirect-uri:}")
    private String redirectUri;

    @Value("${meta.facebook.page-id:}")
    private String configuredPageId;

    @Value("${meta.facebook.page-access-token:}")
    private String configuredPageAccessToken;

    private volatile String runtimePageId;
    private volatile String runtimePageAccessToken;

    public String buildOAuthUrl() {
        ensureAppConfig();
        return "https://www.facebook.com/v20.0/dialog/oauth"
                + "?client_id=" + encode(appId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode("pages_manage_posts,pages_read_engagement,pages_show_list")
                + "&response_type=code";
    }

    public FacebookOAuthResponse exchangeCodeForPageToken(String code) {
        ensureAppConfig();

        Map<String, Object> tokenResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v20.0/oauth/access_token")
                        .queryParam("client_id", appId)
                        .queryParam("client_secret", appSecret)
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("code", code)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (tokenResponse == null || tokenResponse.get("access_token") == null) {
            throw new RuntimeException("Failed to fetch Facebook access token.");
        }

        String userAccessToken = String.valueOf(tokenResponse.get("access_token"));

        Map<String, Object> accountsResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v20.0/me/accounts")
                        .queryParam("access_token", userAccessToken)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (accountsResponse == null || accountsResponse.get("data") == null) {
            throw new RuntimeException("No managed Facebook pages found for this account.");
        }

        List<Map<String, Object>> pages = (List<Map<String, Object>>) accountsResponse.get("data");
        if (pages.isEmpty()) {
            throw new RuntimeException("No Facebook Page available. Make sure you manage at least one page.");
        }

        Map<String, Object> selectedPage = selectPage(pages);
        this.runtimePageId = String.valueOf(selectedPage.get("id"));
        this.runtimePageAccessToken = String.valueOf(selectedPage.get("access_token"));

        return new FacebookOAuthResponse(
                userAccessToken,
                this.runtimePageAccessToken,
                this.runtimePageId,
                String.valueOf(selectedPage.getOrDefault("name", ""))
        );
    }

    public FacebookPublishResponse publishPhoto(FacebookPublishRequest request) {
        String pageId = resolvePageId();
        String pageToken = resolvePageAccessToken();

        if (request == null || request.getImageBase64() == null || request.getImageBase64().isBlank()) {
            throw new RuntimeException("Image data is required for Facebook publishing.");
        }

        byte[] imageBytes = decodeBase64Image(request.getImageBase64());
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("access_token", pageToken);
        bodyBuilder.part("published", "true");
        bodyBuilder.part("caption", request.getMessage() == null ? "" : request.getMessage());
        bodyBuilder.part("source", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "election-result.png";
            }
        }).contentType(MediaType.IMAGE_PNG);

        Map<String, Object> response = webClient.post()
                .uri("/v20.0/" + pageId + "/photos")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Facebook publish failed with empty response.");
        }

        return new FacebookPublishResponse(
                String.valueOf(response.getOrDefault("id", "")),
                String.valueOf(response.getOrDefault("post_id", "")),
                true,
                "Image posted publicly to Facebook page."
        );
    }

    private String resolvePageId() {
        if (runtimePageId != null && !runtimePageId.isBlank()) {
            return runtimePageId;
        }
        if (configuredPageId != null && !configuredPageId.isBlank()) {
            return configuredPageId;
        }
        throw new RuntimeException("Facebook page id is missing. Set meta.facebook.page-id or complete OAuth.");
    }

    private String resolvePageAccessToken() {
        if (runtimePageAccessToken != null && !runtimePageAccessToken.isBlank()) {
            return runtimePageAccessToken;
        }
        if (configuredPageAccessToken != null && !configuredPageAccessToken.isBlank()) {
            return configuredPageAccessToken;
        }
        throw new RuntimeException("Facebook page access token missing. Set meta.facebook.page-access-token or complete OAuth.");
    }

    private Map<String, Object> selectPage(List<Map<String, Object>> pages) {
        return pages.get(0);
    }

    private byte[] decodeBase64Image(String value) {
        String payload = value;
        int commaIndex = value.indexOf(',');
        if (commaIndex >= 0) {
            payload = value.substring(commaIndex + 1);
        }
        return Base64.getDecoder().decode(payload);
    }

    private void ensureAppConfig() {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank() || redirectUri == null || redirectUri.isBlank()) {
            throw new RuntimeException("Meta app config missing. Set meta.facebook.app-id, meta.facebook.app-secret, and meta.facebook.redirect-uri.");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
