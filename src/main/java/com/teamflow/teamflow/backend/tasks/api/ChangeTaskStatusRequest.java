package com.teamflow.teamflow.backend.tasks.api;

import jakarta.validation.constraints.NotNull;

public record ChangeTaskStatusRequest(
        @NotNull String status
) {}
