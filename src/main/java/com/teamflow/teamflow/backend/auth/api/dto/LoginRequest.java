package com.teamflow.teamflow.backend.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String identifier, // either email or username
        @NotBlank String password
) {}
