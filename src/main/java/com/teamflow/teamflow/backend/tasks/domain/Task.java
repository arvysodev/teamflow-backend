package com.teamflow.teamflow.backend.tasks.domain;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Task(UUID projectId, String title, String description, UUID createdBy) {
        this.projectId = projectId;
        this.title = normalizeRequired(title, "Task title must not be blank.");
        this.description = normalizeOptional(description);
        this.createdBy = createdBy;
        this.status = TaskStatus.TODO;
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

    public void updateDetails(String title, String description) {
        this.title = normalizeRequired(title, "Task title must not be blank.");
        this.description = normalizeOptional(description);
    }

    public void changeStatus(TaskStatus newStatus) {
        if (newStatus == null) {
            throw new BadRequestException("Task status must not be null.");
        }
        this.status = newStatus;
    }

    public void assignTo(UUID userId) {
        this.assigneeUserId = userId;
    }

    public void unassign() {
        this.assigneeUserId = null;
    }

    private static String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.strip();
    }

    private static String normalizeOptional(String value) {
        if (value == null) return null;
        String v = value.strip();
        return v.isBlank() ? null : v;
    }
}
