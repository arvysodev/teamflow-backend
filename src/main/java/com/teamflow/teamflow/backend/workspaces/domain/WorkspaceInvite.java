package com.teamflow.teamflow.backend.workspaces.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "workspace_invites")
public class WorkspaceInvite {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WorkspaceInvite(UUID workspaceId,
                           String email,
                           String tokenHash,
                           LocalDateTime expiresAt,
                           UUID createdBy) {
        this.workspaceId = workspaceId;
        this.email = email.toLowerCase().strip();
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public WorkspaceInviteStatus status(LocalDateTime now) {
        if (isAccepted()) return WorkspaceInviteStatus.ACCEPTED;
        if (isExpired(now)) return WorkspaceInviteStatus.EXPIRED;
        return WorkspaceInviteStatus.PENDING;
    }

    public void accept(LocalDateTime now) {
        this.acceptedAt = now;
    }
}
