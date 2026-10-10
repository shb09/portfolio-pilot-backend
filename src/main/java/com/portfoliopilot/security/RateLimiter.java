package com.portfoliopilot.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fixed-window rate limiter for abuse-prone endpoints
 * (login, verification resend). Single-instance scope is fine for
 * this deployment size; counts reset on restart.
 */
@Service
public class RateLimiter {

    private record Bucket(int count, Instant windowStart) {
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @throws ResponseStatusException 429 when maxAttempts within window is exceeded.
     */
    public void check(String scope, String key, int maxAttempts, long windowSeconds) {
        String mapKey = scope + ":" + key.toLowerCase();
        Instant now = Instant.now();
        buckets.compute(mapKey, (k, bucket) -> {
            if (bucket == null || now.isAfter(bucket.windowStart().plusSeconds(windowSeconds))) {
                return new Bucket(1, now);
            }
            if (bucket.count() >= maxAttempts) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many attempts. Please wait and try again.");
            }
            return new Bucket(bucket.count() + 1, bucket.windowStart());
        });
    }
}
