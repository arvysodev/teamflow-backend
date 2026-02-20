package com.teamflow.teamflow.backend.tasks.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTaskRequest(
        @NotNull UUID userId
) {}
