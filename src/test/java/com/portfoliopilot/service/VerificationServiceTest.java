package com.portfoliopilot.service;

import com.portfoliopilot.dto.ForgotRequest;
import com.portfoliopilot.dto.RegisterRequest;
import com.portfoliopilot.dto.ResendRequest;
import com.portfoliopilot.dto.ResetRequest;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.UserRepository;
import com.portfoliopilot.security.RateLimiter;
import com.portfoliopilot.security.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MailService mailService;
    @Mock
    private RateLimiter rateLimiter;

    private VerificationService service;

    @Mock
    private com.portfoliopilot.security.JwtService jwtService;

    @BeforeEach
    void setUp() {
        service = new VerificationService(userRepository, passwordEncoder, mailService,
                rateLimiter, jwtService, 24, 60, true);
    }

    private RegisterRequest req() {
        return new RegisterRequest("asha-01", "Asha", "Asha@Test.com", "password1234");
    }

    @Test
    void registerCreatesActiveAccountWithJwt() {
        when(userRepository.existsByUsername("asha-01")).thenReturn(false);
        when(userRepository.existsByEmail("asha@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password1234")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generate("asha@test.com", com.portfoliopilot.entity.Role.USER)).thenReturn("jwt");

        var res = service.register(req());

        assertEquals("jwt", res.token());
        assertEquals("asha-01", res.user().username());
        assertTrue(res.user().emailVerified());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerSurvivesMailOutage() {
        when(userRepository.existsByUsername("asha-01")).thenReturn(false);
        when(userRepository.existsByEmail("asha@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password1234")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generate("asha@test.com", com.portfoliopilot.entity.Role.USER)).thenReturn("jwt");
        org.mockito.Mockito.doThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "down"))
                .when(mailService).sendWelcome(anyString(), anyString());

        var res = service.register(req());

        assertEquals("jwt", res.token());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("asha-01")).thenReturn(true);

        var ex = assertThrows(ResponseStatusException.class, () -> service.register(req()));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByUsername("asha-01")).thenReturn(false);
        when(userRepository.existsByEmail("asha@test.com")).thenReturn(true);

        var ex = assertThrows(ResponseStatusException.class, () -> service.register(req()));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void verifySuccessMarksVerifiedAndBurnsToken() {
        String raw = TokenUtil.rawToken();
        User user = pendingUser(TokenUtil.sha256(raw), Instant.now().plusSeconds(3600));
        when(userRepository.findByVerificationTokenHash(user.getVerificationTokenHash()))
                .thenReturn(Optional.of(user));

        String message = service.verify(raw);

        assertTrue(user.getEmailVerified());
        assertNull(user.getVerificationTokenHash());
        assertTrue(message.contains("sign in"));
    }

    @Test
    void verifyBadTokenRejected() {
        when(userRepository.findByVerificationTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.verify(TokenUtil.rawToken()));
    }

    @Test
    void verifyExpiredTokenGone() {
        String raw = TokenUtil.rawToken();
        User user = pendingUser(TokenUtil.sha256(raw), Instant.now().minusSeconds(10));
        when(userRepository.findByVerificationTokenHash(user.getVerificationTokenHash()))
                .thenReturn(Optional.of(user));

        var ex = assertThrows(ResponseStatusException.class, () -> service.verify(raw));
        assertEquals(410, ex.getStatusCode().value());
        assertFalse(user.getEmailVerified());
    }

    @Test
    void resendOnVerifiedAccountRejected() {
        User user = pendingUser(null, null);
        user.setEmailVerified(true);
        when(userRepository.findByEmail("asha@test.com")).thenReturn(Optional.of(user));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.resend(new ResendRequest("asha@test.com")));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void forgotIsEnumerationSafe() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        String message = service.forgot(new ForgotRequest("nobody@test.com"));

        assertTrue(message.contains("If an account exists"));
    }

    @Test
    void resetSuccessBurnsToken() {
        String raw = TokenUtil.rawToken();
        User user = pendingUser(null, null);
        user.setEmailVerified(true);
        user.setResetTokenHash(TokenUtil.sha256(raw));
        user.setResetExpiry(Instant.now().plusSeconds(3600));
        when(userRepository.findByResetTokenHash(user.getResetTokenHash())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword1")).thenReturn("newhash");

        service.reset(new ResetRequest(raw, "newpassword1"));

        assertEquals("newhash", user.getPasswordHash());
        assertNull(user.getResetTokenHash());
    }

    private User pendingUser(String tokenHash, Instant expiry) {
        User user = new User();
        user.setUsername("asha-01");
        user.setEmail("asha@test.com");
        user.setEmailVerified(false);
        user.setEnabled(true);
        user.setVerificationTokenHash(tokenHash);
        user.setVerificationExpiry(expiry);
        return user;
    }
}
