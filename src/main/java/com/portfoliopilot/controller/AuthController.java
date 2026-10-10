package com.portfoliopilot.controller;

import com.portfoliopilot.dto.AuthResponse;
import com.portfoliopilot.dto.ForgotRequest;
import com.portfoliopilot.dto.LoginRequest;
import com.portfoliopilot.dto.OAuthExchangeRequest;
import com.portfoliopilot.dto.RegisterRequest;
import com.portfoliopilot.dto.RegisterResponse;
import com.portfoliopilot.dto.ResendRequest;
import com.portfoliopilot.dto.ResetRequest;
import com.portfoliopilot.dto.UserDto;
import com.portfoliopilot.service.AuthService;
import com.portfoliopilot.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Public: register (pending), verify, resend, login, forgot, reset.
 * Authenticated: me. Controllers do HTTP only — rules live in services.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final VerificationService verificationService;
    private final boolean googleEnabled;

    public AuthController(AuthService authService, VerificationService verificationService,
            @Value("${GOOGLE_OAUTH_CLIENT_ID:}") String googleClientId) {
        this.authService = authService;
        this.verificationService = verificationService;
        this.googleEnabled = googleClientId != null && !googleClientId.isBlank();
    }

    /** Creates an ACTIVE account and returns a JWT — login is immediate. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return verificationService.register(request);
    }

    /** Verification links land here: GET /api/auth/verify?token=… */
    @GetMapping("/verify")
    public Map<String, String> verify(@RequestParam String token) {
        return Map.of("message", verificationService.verify(token));
    }

    @PostMapping("/resend")
    public RegisterResponse resend(@Valid @RequestBody ResendRequest request) {
        return verificationService.resend(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot")
    public Map<String, String> forgot(@Valid @RequestBody ForgotRequest request) {
        return Map.of("message", verificationService.forgot(request));
    }

    @PostMapping("/reset")
    public Map<String, String> reset(@Valid @RequestBody ResetRequest request) {
        return Map.of("message", verificationService.reset(request));
    }

    /** Lets the login page show the Google button only when OAuth is configured. */
    @GetMapping("/oauth/status")
    public Map<String, Boolean> oauthStatus() {
        return Map.of("googleEnabled", googleEnabled);
    }

    /** One-time code (from /oauth/callback) → application JWT. Single-use. */
    @PostMapping("/oauth/exchange")
    public AuthResponse oauthExchange(@Valid @RequestBody OAuthExchangeRequest request) {
        return authService.oauthExchange(request.code());
    }

    /** Authentication here is filled by JwtAuthFilter; getName() = email. */
    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authService.me(authentication.getName());
    }
}
