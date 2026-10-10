package com.portfoliopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * One row in `users` = one account. Parent of every other table:
 * each child row carries a user_id FK pointing here.
 *
 * Identity: username (unique, lowercase) + email (unique, lowercase).
 * Security: BCrypt hash only; role is server-controlled; email must be
 * verified before login. Token hashes are single-use and expiring.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Login handle. Stored lowercase; uniqueness enforced (case-insensitive collation).
        Nullable in DDL so existing rows survive the migration; backfilled on boot. */
    @Column(unique = true, length = 50)
    private String username;

    /** Stored lowercase + trimmed on write. Unique. */
    @Column(nullable = false, unique = true, length = 180)
    private String email;

    /** BCrypt hash, never the raw password. Never sent to the frontend. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Role role = Role.USER;

    /** Nullable in DDL so existing rows survive the migration; backfilled on boot. */
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column
    private Boolean enabled = true;

    /** SHA-256 hex of the verification token. Null when none pending. */
    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Column(name = "verification_expiry")
    private Instant verificationExpiry;

    /** SHA-256 hex of the password-reset token. Null when none pending. */
    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;

    @Column(name = "reset_expiry")
    private Instant resetExpiry;

    /**
     * Google OIDC subject (stable provider identity). Nullable so existing
     * rows survive the additive migration; unique when present.
     */
    @Column(name = "google_sub", unique = true, length = 100)
    private String googleSub;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getVerificationTokenHash() {
        return verificationTokenHash;
    }

    public void setVerificationTokenHash(String verificationTokenHash) {
        this.verificationTokenHash = verificationTokenHash;
    }

    public Instant getVerificationExpiry() {
        return verificationExpiry;
    }

    public void setVerificationExpiry(Instant verificationExpiry) {
        this.verificationExpiry = verificationExpiry;
    }

    public String getResetTokenHash() {
        return resetTokenHash;
    }

    public void setResetTokenHash(String resetTokenHash) {
        this.resetTokenHash = resetTokenHash;
    }

    public Instant getResetExpiry() {
        return resetExpiry;
    }

    public void setResetExpiry(Instant resetExpiry) {
        this.resetExpiry = resetExpiry;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
