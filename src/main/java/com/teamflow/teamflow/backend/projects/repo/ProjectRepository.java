package com.teamflow.teamflow.backend.projects.repo;

import com.teamflow.teamflow.backend.projects.domain.Project;
import com.teamflow.teamflow.backend.projects.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findAllByWorkspaceIdAndStatus(
            UUID workspaceId,
            ProjectStatus status,
            Pageable pageable
    );

    Optional<Project> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    boolean existsByWorkspaceIdAndName(UUID workspaceId, String name);
    boolean existsByWorkspaceIdAndNameAndIdNot(UUID workspaceId, String name, UUID id);
}