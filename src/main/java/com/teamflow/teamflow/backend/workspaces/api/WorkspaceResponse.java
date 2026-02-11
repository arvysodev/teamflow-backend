package com.teamflow.teamflow.backend.workspaces.api;

import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        LocalDateTime createdAt,
        WorkspaceStatus status,
        LocalDateTime updatedAt
) {}
