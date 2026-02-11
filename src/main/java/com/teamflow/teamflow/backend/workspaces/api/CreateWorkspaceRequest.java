package com.teamflow.teamflow.backend.workspaces.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest (
        @NotBlank(message = "Name must not be blank.")
        @Size(max=255, message = "Maximum allowed name size is 255 characters.")
        String name
) {}
