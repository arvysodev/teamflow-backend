package com.teamflow.teamflow.backend.projects.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String status,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
