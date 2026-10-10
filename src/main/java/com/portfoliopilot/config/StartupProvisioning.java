package com.portfoliopilot.config;

import com.portfoliopilot.entity.Role;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Startup provisioning (idempotent, env-driven, never logged secrets):
 * 1. Backfill pre-verification accounts (NULL verified -> verified USER)
 *    so existing local data keeps working after the verification release.
 * 2. Create the admin account ONLY when APP_ADMIN_* are all set and no
 *    account uses that email. Existing accounts are never modified.
 */
@Component
public class StartupProvisioning implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupProvisioning.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminUsername;
    private final String adminPassword;

    public StartupProvisioning(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.username:}") String adminUsername,
            @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
        this.adminUsername = adminUsername == null ? "" : adminUsername.trim().toLowerCase();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long backfilled = userRepository.findAll().stream()
                .filter(u -> u.getEmailVerified() == null || u.getUsername() == null)
                .peek(u -> {
                    if (u.getUsername() == null) {
                        u.setUsername(claimUsername(u.getEmail()));
                    }
                    if (u.getEmailVerified() == null) {
                        u.setEmailVerified(true);
                    }
                    if (u.getRole() == null) {
                        u.setRole(Role.USER);
                    }
                    if (u.getEnabled() == null) {
                        u.setEnabled(true);
                    }
                })
                .count();
        if (backfilled > 0) {
            userRepository.flush();
            log.info("Backfilled {} pre-verification accounts (username + verified USER) as one-time migration", backfilled);
        }

        if (adminPassword.isEmpty() || adminEmail.isEmpty() || adminUsername.isEmpty()) {
            log.info("Admin bootstrap skipped (APP_ADMIN_* not fully configured)");
            return;
        }
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin bootstrap skipped: an account already uses the configured email; left unchanged");
            return;
        }
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            log.warn("Admin bootstrap skipped: configured username is taken by another account");
            return;
        }
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setName("Administrator");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEmailVerified(true);
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("Admin account provisioned for username='{}' (operator-configured)", adminUsername);
    }

    /** Derives a unique lowercase handle from the email local-part. */
    private String claimUsername(String email) {
        String base = email == null ? "user" : email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (base.length() < 3) {
            base = (base + "user").substring(0, 3);
        }
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }
        String candidate = base;
        int suffix = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
