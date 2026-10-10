package com.portfoliopilot.service;

import com.portfoliopilot.dto.ForgotRequest;
import com.portfoliopilot.dto.RegisterRequest;
import com.portfoliopilot.dto.RegisterResponse;
import com.portfoliopilot.dto.ResendRequest;
import com.portfoliopilot.dto.ResetRequest;
import com.portfoliopilot.entity.Role;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.UserRepository;
import com.portfoliopilot.security.RateLimiter;
import com.portfoliopilot.security.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Pending registration, email verification, resend and password reset.
 * Tokens: 256-bit random, SHA-256 stored, expiring, single-use.
 * Raw tokens never touch logs or the database.
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final RateLimiter rateLimiter;
    private final long verificationTtlHours;
    private final long resetTtlMinutes;
    private final boolean devMode;

    public VerificationService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            RateLimiter rateLimiter,
            @Value("${app.mail.verification-ttl-hours:24}") long verificationTtlHours,
            @Value("${app.mail.reset-ttl-minutes:60}") long resetTtlMinutes,
            @Value("${app.auth.dev-mode:false}") boolean devMode) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.rateLimiter = rateLimiter;
        this.verificationTtlHours = verificationTtlHours;
        this.resetTtlMinutes = resetTtlMinutes;
        this.devMode = devMode;
    }

    /** Creates a PENDING account (never authenticated) + sends verification. */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setUsername(username);
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEmailVerified(false);
        user.setEnabled(true);
        String rawToken = TokenUtil.rawToken();
        user.setVerificationTokenHash(TokenUtil.sha256(rawToken));
        user.setVerificationExpiry(Instant.now().plusSeconds(verificationTtlHours * 3600));
        userRepository.save(user);
        log.info("Registered pending account username='{}'", username);

        return deliverVerification(user, rawToken);
    }

    /** Single-use, expiring verification. Marks verified + confirms by mail. */
    @Transactional
    public String verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token is required");
        }
        String hash = TokenUtil.sha256(rawToken.trim());
        User user = userRepository.findByVerificationTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This verification link is invalid or has already been used."));
        if (user.getVerificationExpiry() == null || Instant.now().isAfter(user.getVerificationExpiry())) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "This verification link has expired. Request a new one from the login page.");
        }
        user.setEmailVerified(true);
        user.setVerificationTokenHash(null);
        user.setVerificationExpiry(null);
        userRepository.save(user);
        log.info("Email verified username='{}'", user.getUsername());
        if (mailService.isConfigured()) {
            try {
                mailService.sendVerifiedConfirmation(user.getEmail(), user.getUsername());
            } catch (ResponseStatusException e) {
                log.warn("Confirmation mail failed for username='{}'", user.getUsername());
            }
        }
        return "Email verified. You can now sign in with your username or email address.";
    }

    /** Regenerates the token (old link dies) and resends. Rate-limited. */
    @Transactional
    public RegisterResponse resend(ResendRequest request) {
        String email = request.email().trim().toLowerCase();
        rateLimiter.check("resend", email, 3, 3600);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No pending account found for this email."));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This email is already verified. Please sign in.");
        }
        String rawToken = TokenUtil.rawToken();
        user.setVerificationTokenHash(TokenUtil.sha256(rawToken));
        user.setVerificationExpiry(Instant.now().plusSeconds(verificationTtlHours * 3600));
        userRepository.save(user);
        log.info("Verification re-issued username='{}'", user.getUsername());
        return deliverVerification(user, rawToken);
    }

    /**
     * Always returns the same message (no account enumeration). Creates a
     * reset token only for verified, enabled accounts.
     */
    @Transactional
    public String forgot(ForgotRequest request) {
        String email = request.email().trim().toLowerCase();
        rateLimiter.check("forgot", email, 3, 3600);
        userRepository.findByEmail(email)
                .filter(u -> Boolean.TRUE.equals(u.getEmailVerified()) && Boolean.TRUE.equals(u.getEnabled()))
                .ifPresent(user -> {
                    String rawToken = TokenUtil.rawToken();
                    user.setResetTokenHash(TokenUtil.sha256(rawToken));
                    user.setResetExpiry(Instant.now().plusSeconds(resetTtlMinutes * 60));
                    userRepository.save(user);
                    try {
                        mailService.sendPasswordReset(user.getEmail(), user.getUsername(), rawToken, resetTtlMinutes);
                    } catch (ResponseStatusException e) {
                        log.warn("Reset mail failed username='{}'", user.getUsername());
                    }
                    log.info("Password reset issued username='{}'", user.getUsername());
                });
        return "If an account exists for this email, a reset link has been sent.";
    }

    /** Single-use reset: validates token, sets new hash, kills all tokens. */
    @Transactional
    public String reset(ResetRequest request) {
        if (request.token() == null || request.token().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token is required");
        }
        String hash = TokenUtil.sha256(request.token().trim());
        User user = userRepository.findByResetTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This reset link is invalid or has already been used."));
        if (user.getResetExpiry() == null || Instant.now().isAfter(user.getResetExpiry())) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "This reset link has expired. Request a new one.");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is disabled.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetTokenHash(null);
        user.setResetExpiry(null);
        userRepository.save(user);
        log.info("Password reset completed username='{}'", user.getUsername());
        return "Password updated. You can now sign in with your new password.";
    }

    private RegisterResponse deliverVerification(User user, String rawToken) {
        String message = "Account created for '" + user.getUsername()
                + "'. Check your email to verify your address, then sign in.";
        if (!mailService.isConfigured()) {
            log.warn("Mail not configured; verification pending username='{}'", user.getUsername());
            if (devMode) {
                return RegisterResponse.pendingDev(
                        message + " (DEV MODE: email not configured — use the provided token.)", rawToken);
            }
            return RegisterResponse.pending(
                    message + " Email delivery is currently unavailable; use Resend verification later.");
        }
        mailService.sendVerification(user.getEmail(), user.getUsername(), rawToken, verificationTtlHours);
        if (devMode) {
            return RegisterResponse.pendingDev(message, rawToken);
        }
        return RegisterResponse.pending(message);
    }
}
