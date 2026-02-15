package com.teamflow.teamflow.backend.auth.api.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String status,
        String message
) {}
