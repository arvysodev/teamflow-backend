package com.teamflow.teamflow.backend.auth.api.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}
