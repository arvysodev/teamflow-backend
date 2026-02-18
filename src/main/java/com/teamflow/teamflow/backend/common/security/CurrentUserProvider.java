package com.teamflow.teamflow.backend.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider {

    private AuthenticatedUser principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated principal found.");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Unexpected principal type.");
        }

        return user;
    }

    public UUID getCurrentUserId() {
        return principal().id();
    }

    public String getCurrentUserEmail() {
        return principal().email();
    }

    public String getCurrentUserRole() {
        return principal().role();
    }
}
