package com.teamflow.teamflow.backend.workspaces.domain;

import com.teamflow.teamflow.backend.users.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "workspace_members")
public class WorkspaceMember {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private WorkspaceMemberId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private WorkspaceMemberRole role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void prePersist() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
    }

    public WorkspaceMember(UUID workspaceId, UUID userId, WorkspaceMemberRole role) {
        this.id = new WorkspaceMemberId(workspaceId, userId);
        this.role = role;
    }

    public static WorkspaceMember owner(UUID workspaceId, UUID userId) {
        return new WorkspaceMember(workspaceId, userId, WorkspaceMemberRole.OWNER);
    }

}
