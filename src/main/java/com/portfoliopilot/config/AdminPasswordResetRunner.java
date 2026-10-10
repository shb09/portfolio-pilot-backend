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
 * Explicit, local-only administrator password reset.
 *
 * Runs ONLY when the process is started with the program argument
 * {@code --reset-admin-password} AND the new password is supplied via the
 * {@code APP_ADMIN_NEW_PASSWORD} environment variable. Normal boots
 * (no flag) are a strict no-op — this never resets anything by itself.
 *
 * Safety rules:
 * - targets exactly one account: {@code APP_ADMIN_EMAIL} must resolve to a
 *   single existing user whose role is ADMIN (email is UNIQUE in TiDB);
 * - updates ONLY the {@code password_hash} column via the configured
 *   {@link PasswordEncoder} (BCrypt); every other field is untouched;
 * - the plaintext password and its hash are never logged, never committed,
 *   and never exposed through any API.
 */
@Component
public class AdminPasswordResetRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminPasswordResetRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;

    public AdminPasswordResetRunner(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!args.containsOption("reset-admin-password")) {
            return;
        }
        if (adminEmail.isEmpty()) {
            throw new IllegalStateException(
                    "APP_ADMIN_EMAIL must be set to the administrator's email to run --reset-admin-password");
        }
        String raw = System.getenv("APP_ADMIN_NEW_PASSWORD");
        if (raw == null || raw.length() < 10 || raw.length() > 100) {
            throw new IllegalStateException(
                    "APP_ADMIN_NEW_PASSWORD must hold a 10-100 character password (environment only — never commit it, unset it afterwards)");
        }
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "No account found for APP_ADMIN_EMAIL; nothing was changed"));
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalStateException(
                    "Refusing: account '" + adminEmail + "' is not ADMIN (role=" + admin.getRole() + "); nothing was changed");
        }
        admin.setPasswordHash(passwordEncoder.encode(raw));
        userRepository.save(admin);
        log.info("Admin password hash updated for username='{}' (no other fields touched)", admin.getUsername());
    }
}
