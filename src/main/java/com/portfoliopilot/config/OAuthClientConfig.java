package com.portfoliopilot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Google OIDC registration.
 *
 * Credentials come ONLY from GOOGLE_OAUTH_CLIENT_ID / GOOGLE_OAUTH_CLIENT_SECRET.
 * Without them the repository resolves nothing and the app runs password-only
 * (the login page hides the Google button via /api/auth/oauth/status).
 *
 * Why programmatic: Spring Boot rejects an OAuth registration with an empty
 * client-id at startup, so a placeholder property would break every boot
 * without credentials. No secrets live in properties or code.
 */
@Configuration
public class OAuthClientConfig {

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    public ClientRegistrationRepository clientRegistrationRepository() {
        String id = System.getenv().getOrDefault("GOOGLE_OAUTH_CLIENT_ID", "");
        String secret = System.getenv().getOrDefault("GOOGLE_OAUTH_CLIENT_SECRET", "");
        if (id.isBlank()) {
            return registrationId -> null;
        }
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(id)
                .clientSecret(secret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
