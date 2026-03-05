package com.teamflow.teamflow.backend.projects.repo;

import com.teamflow.teamflow.backend.projects.domain.Project;
import com.teamflow.teamflow.backend.projects.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("""
        select p from Project p
        where p.workspaceId = :workspaceId
          and p.status = :status
          and (:q is null or lower(p.name) like lower(concat('%', :q, '%')))
        """)
    Page<Project> search(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ProjectStatus status,
            @Param("q") String q,
            Pageable pageable
    );

    Page<Project> findAllByWorkspaceIdAndStatus(
            UUID workspaceId,
            ProjectStatus status,
            Pageable pageable
    );

    Optional<Project> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    boolean existsByWorkspaceIdAndName(UUID workspaceId, String name);
    boolean existsByWorkspaceIdAndNameAndIdNot(UUID workspaceId, String name, UUID id);
}