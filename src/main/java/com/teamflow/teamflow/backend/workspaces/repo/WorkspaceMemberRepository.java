package com.teamflow.teamflow.backend.workspaces.repo;

import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMember;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {
    boolean existsByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);
    List<WorkspaceMember> findByIdWorkspaceIdOrderByRoleAscJoinedAtAsc(UUID workspaceId);
}
