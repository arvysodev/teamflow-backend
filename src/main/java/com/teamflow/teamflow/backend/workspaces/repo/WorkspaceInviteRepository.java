package com.teamflow.teamflow.backend.workspaces.repo;

import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, UUID> {

    boolean existsByWorkspaceIdAndEmailAndAcceptedAtIsNullAndExpiresAtAfter(
            UUID workspaceId,
            String email,
            LocalDateTime now
    );

    Optional<WorkspaceInvite> findTokenByHash(String tokenHash);
}
