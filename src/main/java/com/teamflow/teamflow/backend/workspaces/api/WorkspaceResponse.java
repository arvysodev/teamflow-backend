package com.teamflow.teamflow.backend.workspaces.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        LocalDateTime createdAt
) {}
