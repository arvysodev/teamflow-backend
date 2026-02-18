package com.teamflow.teamflow.backend.workspaces.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID userId,
        String role,
        LocalDateTime joinedAt
) {}
