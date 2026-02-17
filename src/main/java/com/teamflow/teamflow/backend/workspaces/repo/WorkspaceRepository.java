package com.teamflow.teamflow.backend.workspaces.repo;

import com.teamflow.teamflow.backend.workspaces.domain.Workspace;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
    Page<Workspace> findByStatus(WorkspaceStatus status, Pageable pageable);

    @Query("""
        select w
        from Workspace w
        join WorkspaceMember wm on wm.id.workspaceId = w.id
        where wm.id.userId = :userId and w.status = :status
        """)
    Page<Workspace> findAllByMemberAndStatus(
            @Param("userId") UUID userId,
            @Param("status") WorkspaceStatus status,
            Pageable pageable
    );

    @Query("""
        select w
        from Workspace w
        join WorkspaceMember wm on wm.id.workspaceId = w.id
        where w.id = :workspaceId and wm.id.userId = :userId
        """)
    Optional<Workspace> findByIdAndMember(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId
    );
}
