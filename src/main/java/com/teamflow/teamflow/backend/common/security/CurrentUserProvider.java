package com.teamflow.teamflow.backend.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider {

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated principal found.");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof String userIdString)) {
            throw new IllegalStateException("Unexpected principal type.");
        }

        try {
            return UUID.fromString(userIdString);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid user ID in security context.");
        }
    }
}
