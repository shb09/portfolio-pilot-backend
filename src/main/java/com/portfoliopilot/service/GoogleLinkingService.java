package com.portfoliopilot.service;

import com.portfoliopilot.entity.Role;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.UserRepository;
import com.portfoliopilot.security.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Google OIDC → local account resolution.
 *
 * Rules (takeover-safe):
 * - Google email must be verified by Google (OIDC email_verified claim).
 * - Known google_sub → that account (must be enabled; ADMIN accounts are
 *   refused here — administrators sign in with their password).
 * - Unknown sub + known email → 409. We never auto-link a password account
 *   on email match alone; the owner keeps password login untouched.
 * - Unknown sub + unknown email → create a fresh USER (verified, enabled,
 *   unusable password hash, never ADMIN).
 */
@Service
public class GoogleLinkingService {

    private static final Logger log = LoggerFactory.getLogger(GoogleLinkingService.class);

    private final UserRepository userRepository;

    public GoogleLinkingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User resolve(OidcUser oidc) {
        String sub = oidc.getSubject();
        String email = oidc.getEmail() == null ? "" : oidc.getEmail().trim().toLowerCase();
        if (sub == null || sub.isBlank() || email.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account has no usable identity");
        }
        if (!Boolean.TRUE.equals(oidc.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
        }

        var known = userRepository.findByGoogleSub(sub);
        if (known.isPresent()) {
            User user = known.get();
            if (!Boolean.TRUE.equals(user.getEnabled())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is disabled. Contact support.");
            }
            if (user.getRole() == Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Administrators must sign in with their password.");
            }
            return user;
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists. Sign in with your password — it keeps working.");
        }

        User user = new User();
        user.setUsername(uniqueUsername(email.split("@")[0]));
        String fullName = oidc.getFullName();
        user.setName(fullName == null || fullName.isBlank() ? user.getUsername() : fullName.trim());
        user.setEmail(email);
        user.setPasswordHash("{google-oauth}" + TokenUtil.rawToken());
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setGoogleSub(sub);
        try {
            User saved = userRepository.save(user);
            log.info("Google-linked account created username='{}'", saved.getUsername());
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists. Sign in with your password — it keeps working.");
        }
    }

    private String uniqueUsername(String base) {
        String clean = base == null ? "" : base.toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (clean.length() < 3) {
            clean = (clean + "user").substring(0, 3);
        }
        if (clean.length() > 24) {
            clean = clean.substring(0, 24);
        }
        String candidate = clean;
        int suffix = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = clean + suffix++;
        }
        return candidate;
    }
}
