package com.teamflow.teamflow.backend.projects.api;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(
        @NotBlank String name
) {}
