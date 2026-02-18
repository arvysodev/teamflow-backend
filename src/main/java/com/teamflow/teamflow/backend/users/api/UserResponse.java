package com.teamflow.teamflow.backend.users.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        String role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
