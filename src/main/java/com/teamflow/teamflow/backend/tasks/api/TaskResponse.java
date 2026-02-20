package com.teamflow.teamflow.backend.tasks.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        String status,
        UUID assigneeUserId,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
