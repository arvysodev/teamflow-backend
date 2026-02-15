package com.teamflow.teamflow.backend.users.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {

    public User(String username, String email, String passwordHash) {
        this.username = username.strip();
        this.email = email.toLowerCase().strip();
        this.passwordHash = passwordHash;

        this.role = UserRole.USER;
        this.status = UserStatus.PENDING;
    }

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "email_verification_token_hash")
    private String emailVerificationTokenHash;

    @Column(name = "email_verification_token_expires_at")
    private LocalDateTime emailVerificationTokenExpiresAt;

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void startEmailVerification(String tokenHash, LocalDateTime expiresAt) {
        this.emailVerificationTokenHash = tokenHash;
        this.emailVerificationTokenExpiresAt = expiresAt;
    }

    public void verifyEmail(LocalDateTime verifiedAt) {
        this.emailVerifiedAt = verifiedAt;
        this.status = UserStatus.ACTIVE;

        this.emailVerificationTokenHash = null;
        this.emailVerificationTokenExpiresAt = null;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (role == null) role = UserRole.USER;
        if (status == null) status = UserStatus.PENDING;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void disable() {
        status = UserStatus.DISABLED;
    }

    public void activate() {
        status = UserStatus.ACTIVE;
    }

    public void promoteToAdmin() {
        role = UserRole.ADMIN;
    }
}
