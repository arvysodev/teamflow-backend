package com.teamflow.teamflow.backend.projects.domain;

import com.teamflow.teamflow.backend.common.errors.ConflictException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "projects",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_projects_workspace_name", columnNames = {"workspace_id", "name"})
        }
)
public class Project {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Project(UUID workspaceId, String name, UUID createdBy) {
        this.workspaceId = workspaceId;
        this.name = name.strip();
        this.createdBy = createdBy;
        this.status = ProjectStatus.ACTIVE;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void rename(String newName) {
        if (status == ProjectStatus.ARCHIVED) {
            throw new ConflictException("Archived project cannot be renamed.");
        }
        this.name = newName.strip();
    }

    public void archive() {
        if (status == ProjectStatus.ARCHIVED) {
            throw new ConflictException("Project is already archived.");
        }
        status = ProjectStatus.ARCHIVED;
    }

    public void restore() {
        if (status == ProjectStatus.ACTIVE) {
            throw new ConflictException("Project is already active.");
        }
        status = ProjectStatus.ACTIVE;
    }
}