package com.portfoliopilot.security;

import com.portfoliopilot.entity.User;
import com.portfoliopilot.service.GoogleLinkingService;
import com.portfoliopilot.service.HandoffStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * OAuth2 login outcome handling. Protocol duties (issuer, audience,
 * signature, state, nonce) are enforced by Spring Security's OIDC support
 * before these handlers run. On success we mint a one-time handoff code and
 * redirect to the frontend — the application JWT is never placed in a URL.
 */
public final class OAuthHandlers {

    private OAuthHandlers() {
    }

    @Component
    public static class Success implements AuthenticationSuccessHandler {

        private static final Logger log = LoggerFactory.getLogger(Success.class);

        private final GoogleLinkingService linkingService;
        private final HandoffStore handoffStore;
        private final String baseUrl;

        public Success(GoogleLinkingService linkingService, HandoffStore handoffStore,
                @Value("${app.base-url}") String baseUrl) {
            this.linkingService = linkingService;
            this.handoffStore = handoffStore;
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                Authentication authentication) throws IOException {
            try {
                OidcUser oidc = (OidcUser) authentication.getPrincipal();
                User user = linkingService.resolve(oidc);
                String code = handoffStore.create(user.getEmail());
                log.info("OAuth handoff issued username='{}'", user.getUsername());
                response.sendRedirect(baseUrl + "/oauth/callback?code=" + code);
            } catch (ResponseStatusException e) {
                log.info("OAuth login refused: {}", e.getStatusCode());
                response.sendRedirect(baseUrl + "/login?oauth=" + reasonOf(e.getStatusCode()));
            }
        }

        private String reasonOf(org.springframework.http.HttpStatusCode status) {
            if (status == HttpStatus.CONFLICT) {
                return "exists";
            }
            if (status == HttpStatus.FORBIDDEN) {
                return "denied";
            }
            return "error";
        }
    }

    @Component
    public static class Failure implements AuthenticationFailureHandler {

        private final String baseUrl;

        public Failure(@Value("${app.base-url}") String baseUrl) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }

        /** Cancelled, expired or invalid provider flow — back to login, no secrets. */
        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.core.AuthenticationException exception) throws IOException {
            response.sendRedirect(baseUrl + "/login?oauth=cancelled");
        }
    }
}
