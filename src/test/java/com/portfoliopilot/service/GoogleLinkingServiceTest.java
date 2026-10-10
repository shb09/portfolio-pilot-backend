package com.portfoliopilot.service;

import com.portfoliopilot.entity.Role;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/** Provider responses are mocked — these prove linking rules, not Google. */
@ExtendWith(MockitoExtension.class)
class GoogleLinkingServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GoogleLinkingService service;

    private OidcUser google(String sub, String email, boolean verified, String name) {
        // Lenient: not every path touches every claim (e.g. known-subject skips the name).
        OidcUser oidc = mock(OidcUser.class, withSettings().lenient());
        when(oidc.getSubject()).thenReturn(sub);
        when(oidc.getEmail()).thenReturn(email);
        when(oidc.getEmailVerified()).thenReturn(verified);
        when(oidc.getFullName()).thenReturn(name);
        return oidc;
    }

    @Test
    void newGoogleUserCreatedAsVerifiedUser() {
        when(userRepository.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("asha@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("asha")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User user = service.resolve(google("sub-1", "Asha@Gmail.com", true, "Asha Sharma"));

        assertEquals(Role.USER, user.getRole());
        assertTrue(user.getEmailVerified());
        assertEquals("asha@gmail.com", user.getEmail());
        assertEquals("sub-1", user.getGoogleSub());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void knownSubjectAuthenticates() {
        User existing = new User();
        existing.setUsername("asha");
        existing.setEmail("asha@gmail.com");
        existing.setRole(Role.USER);
        existing.setEnabled(true);
        when(userRepository.findByGoogleSub("sub-1")).thenReturn(Optional.of(existing));

        assertEquals(existing, service.resolve(google("sub-1", "asha@gmail.com", true, "Asha")));
    }

    @Test
    void collidingEmailNeverAutoLinked() {
        User passwordUser = new User();
        passwordUser.setUsername("asha");
        when(userRepository.findByGoogleSub("sub-9")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("asha@gmail.com")).thenReturn(Optional.of(passwordUser));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.resolve(google("sub-9", "asha@gmail.com", true, "Asha")));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void unverifiedGoogleEmailRejected() {
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.resolve(google("sub-1", "asha@gmail.com", false, "Asha")));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void disabledAccountRejected() {
        User user = new User();
        user.setEnabled(false);
        when(userRepository.findByGoogleSub("sub-1")).thenReturn(Optional.of(user));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.resolve(google("sub-1", "asha@gmail.com", true, "Asha")));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void adminNeverViaOauth() {
        User admin = new User();
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        when(userRepository.findByGoogleSub("sub-1")).thenReturn(Optional.of(admin));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.resolve(google("sub-1", "admin@gmail.com", true, "Admin")));
        assertEquals(403, ex.getStatusCode().value());
    }
}
