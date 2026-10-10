package com.portfoliopilot.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Cryptographic helpers for single-use email tokens. Raw tokens never persist. */
public final class TokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenUtil() {
    }

    /** 32 random bytes as 64 hex chars — sent to the user, never stored. */
    public static String rawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** SHA-256 hex — what actually lands in TiDB. */
    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
