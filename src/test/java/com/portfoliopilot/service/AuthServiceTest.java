package com.portfoliopilot.service;

import com.portfoliopilot.dto.LoginRequest;
import com.portfoliopilot.entity.Role;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.UserRepository;
import com.portfoliopilot.security.JwtService;
import com.portfoliopilot.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private com.portfoliopilot.service.HandoffStore handoffStore;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("asha");
        user.setEmail("asha@test.com");
        user.setPasswordHash("hash");
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        user.setEnabled(true);
    }

    @Test
    void loginByUsername() {
        when(userRepository.findByUsername("asha")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw12345678", "hash")).thenReturn(true);
        when(jwtService.generate("asha@test.com", Role.USER)).thenReturn("jwt");

        var res = authService.login(new LoginRequest("asha", "pw12345678"));

        assertEquals("jwt", res.token());
        assertEquals("asha", res.user().username());
    }

    @Test
    void loginByEmail() {
        when(userRepository.findByUsername("asha@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("asha@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw12345678", "hash")).thenReturn(true);
        when(jwtService.generate("asha@test.com", Role.USER)).thenReturn("jwt");

        var res = authService.login(new LoginRequest("Asha@Test.com", "pw12345678"));

        assertEquals("jwt", res.token());
    }

    @Test
    void wrongPasswordGivesGeneric401() {
        when(userRepository.findByUsername("asha")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        var ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("asha", "nope")));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void unknownIdentifierGivesSame401() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("ghost", "pw12345678")));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void unverifiedLoginRejected403() {
        user.setEmailVerified(false);
        when(userRepository.findByUsername("asha")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        var ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("asha", "pw12345678")));
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("not verified"));
    }

    @Test
    void disabledLoginRejected403() {
        user.setEnabled(false);
        when(userRepository.findByUsername("asha")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        var ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("asha", "pw12345678")));
        assertEquals(403, ex.getStatusCode().value());
    }
}
