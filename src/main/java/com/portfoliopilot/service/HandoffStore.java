package com.portfoliopilot.service;

import com.portfoliopilot.security.TokenUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, single-use authorization handoff for OAuth login.
 * The browser receives only a random code (5-minute TTL); the code is
 * exchanged server-side for the application JWT. Replays fail: consume
 * removes the code atomically, so a second use finds nothing.
 */
@Service
public class HandoffStore {

    private record Entry(String email, Instant expires) {
    }

    private static final long TTL_SECONDS = 300;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    public String create(String email) {
        purge();
        String code = TokenUtil.rawToken();
        codes.put(code, new Entry(email, Instant.now().plusSeconds(TTL_SECONDS)));
        return code;
    }

    public Optional<String> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        purge();
        Entry entry = codes.remove(code.trim());
        if (entry == null || Instant.now().isAfter(entry.expires())) {
            return Optional.empty();
        }
        return Optional.of(entry.email());
    }

    private void purge() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(e -> now.isAfter(e.getValue().expires()));
    }
}
