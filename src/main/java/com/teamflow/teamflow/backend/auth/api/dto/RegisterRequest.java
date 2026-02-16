package com.teamflow.teamflow.backend.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(
                regexp = "^[a-z0-9._-]{3,50}$",
                message = "Username must be 3-50 chars and contain only lowercase letters," +
                        " numbers, dot, underscore or hyphen."
        )
        String username,

        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
