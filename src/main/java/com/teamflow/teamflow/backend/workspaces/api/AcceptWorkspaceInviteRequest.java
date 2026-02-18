package com.teamflow.teamflow.backend.workspaces.api;

import jakarta.validation.constraints.NotBlank;

public record AcceptWorkspaceInviteRequest(
        @NotBlank String rawToken
) {}
