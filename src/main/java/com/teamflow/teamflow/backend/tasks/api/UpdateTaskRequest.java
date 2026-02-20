package com.teamflow.teamflow.backend.tasks.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateTaskRequest(
        @NotBlank String title,
        String description
) {}
