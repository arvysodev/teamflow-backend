package com.teamflow.teamflow.backend.workspaces.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceInviteRequest(
        @NotBlank @Email String email
) {}
