package com.teamflow.teamflow.backend.workspaces.repo;

import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMember;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberId;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {
    boolean existsByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);
    List<WorkspaceMember> findByIdWorkspaceIdOrderByRoleAscJoinedAtAsc(UUID workspaceId);

    @Query("""
        select wm.role from WorkspaceMember wm
        where wm.id.workspaceId = :workspaceId and wm.id.userId = :userId
        """)
    Optional<WorkspaceMemberRole> findRole(UUID workspaceId, UUID userId);

}
