package com.teamflow.teamflow.backend.users.service;

import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.common.security.AuthenticatedUser;
import com.teamflow.teamflow.backend.users.domain.User;
import com.teamflow.teamflow.backend.users.repo.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated principal found.");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AuthenticatedUser authUser)) {
            throw new IllegalStateException("Unexpected principal type.");
        }

        UUID userId = authUser.id();

        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }
}
