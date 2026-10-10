package com.portfoliopilot.security;

import com.portfoliopilot.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and reads JWTs. Token subject = user email.
 * HMAC key built from app.jwt.secret; jjwt picks HS512 when the key
 * is >= 64 bytes (our default), HS256 for 32-63 bytes. Startup fails
 * fast if the secret is shorter than 32 bytes.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generate(String email, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role == null ? Role.USER.name() : role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /** Legacy overload: subject-only tokens default to USER. */
    public String generate(String email) {
        return generate(email, Role.USER);
    }

    /** Throws JwtException (expired, tampered, malformed) on any invalid token. */
    public String subject(String token) {
        return claims(token).getSubject();
    }

    public String role(String token) {
        Object role = claims(token).get("role");
        return role == null ? Role.USER.name() : String.valueOf(role);
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
