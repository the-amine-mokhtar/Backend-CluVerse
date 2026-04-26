package com.hexaweb.backendcluverse.config.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles failed OAuth2 authentication
 */
@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        String errorMessage = extractErrorMessage(exception);
        String redirectUrl = appBaseUrl + "/auth/login?error=" + errorMessage;

        response.sendRedirect(redirectUrl);
    }

    private String extractErrorMessage(AuthenticationException exception) {
        if (exception != null && exception.getMessage() != null) {
            return exception.getMessage().replace(" ", "%20");
        }
        return "Authentication%20failed";
    }
}
