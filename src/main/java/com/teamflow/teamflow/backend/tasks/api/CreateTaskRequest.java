package com.teamflow.teamflow.backend.tasks.api;

import jakarta.validation.constraints.NotBlank;

public record CreateTaskRequest(
        @NotBlank String title,
        String description
) {}
