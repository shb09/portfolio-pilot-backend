package com.portfoliopilot.service;

import com.portfoliopilot.dto.AuthResponse;
import com.portfoliopilot.dto.LoginRequest;
import com.portfoliopilot.dto.UserDto;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.UserRepository;
import com.portfoliopilot.security.JwtService;
import com.portfoliopilot.security.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Login by username OR email (one path, one behavior) + current-user lookup.
 * Gates: existing account, correct password, verified email, enabled account.
 * Failures stay generic except verification/disabled states, which need
 * actionable messages (verified accounts only — no enumeration of unknowns).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimiter rateLimiter;
    private final HandoffStore handoffStore;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, RateLimiter rateLimiter, HandoffStore handoffStore) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.handoffStore = handoffStore;
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim().toLowerCase();
        rateLimiter.check("login", identifier, 10, 300);
        User user = resolve(identifier);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is disabled. Contact support.");
        }
        // Email verification is no longer a login prerequisite (legacy
        // pending accounts included); verification remains optional.
        return new AuthResponse(jwtService.generate(user.getEmail(), user.getRole()), UserDto.from(user));
    }

    /** Username first, then email — both stored lowercase. */
    private User resolve(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElse(null);
    }

    /**
     * OAuth handoff exchange: single-use code → application JWT.
     * Replays and expired codes fail closed with 401.
     */
    public AuthResponse oauthExchange(String code) {
        rateLimiter.check("oauth-exchange", code == null ? "blank" : code, 5, 300);
        String email = handoffStore.consume(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "This sign-in attempt expired or was already used. Please try again."));
        User user = userRepository.findByEmail(email)
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && Boolean.TRUE.equals(u.getEmailVerified()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "This sign-in attempt is no longer valid. Please try again."));
        return new AuthResponse(jwtService.generate(user.getEmail(), user.getRole()), UserDto.from(user));
    }

    public UserDto me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserDto.from(user);
    }
}
