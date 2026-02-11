package com.teamflow.teamflow.backend.workspaces.domain;

import com.teamflow.teamflow.backend.common.errors.ConflictException;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkspaceStatus status;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Workspace() {
    }

    public Workspace(String name) {
        this.name = name.strip();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void close() {
        this.status = WorkspaceStatus.CLOSED;
    }

    public void restore() {
        this.status = WorkspaceStatus.ACTIVE;
    }

    public void rename(String newName) {
        if (status == WorkspaceStatus.CLOSED) {
            throw new ConflictException("Closed workspace cannot be renamed.");
        }

        this.name = newName == null ? null : newName.strip();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = WorkspaceStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
