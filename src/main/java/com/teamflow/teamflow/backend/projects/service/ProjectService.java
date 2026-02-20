package com.teamflow.teamflow.backend.projects.service;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.ConflictException;
import com.teamflow.teamflow.backend.common.errors.ForbiddenException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.common.security.CurrentUserProvider;
import com.teamflow.teamflow.backend.projects.domain.Project;
import com.teamflow.teamflow.backend.projects.domain.ProjectStatus;
import com.teamflow.teamflow.backend.projects.repo.ProjectRepository;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberRole;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserProvider currentUserProvider;

    public ProjectService(
            ProjectRepository projectRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.projectRepository = projectRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public Project create(UUID workspaceId, String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Project name must not be blank.");
        }

        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);

        String normalized = name.strip();

        if (projectRepository.existsByWorkspaceIdAndName(workspaceId, normalized)) {
            throw new ConflictException("Project with this name already exists in this workspace.");
        }

        Project project = new Project(workspaceId, normalized, userId);
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Page<Project> listActive(UUID workspaceId, Pageable pageable) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);

        return projectRepository.findAllByWorkspaceIdAndStatus(workspaceId, ProjectStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Project> listArchived(UUID workspaceId, Pageable pageable) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);

        return projectRepository.findAllByWorkspaceIdAndStatus(workspaceId, ProjectStatus.ARCHIVED, pageable);
    }

    @Transactional(readOnly = true)
    public Project getById(UUID workspaceId, UUID projectId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);

        return projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Project not found."));
    }

    @Transactional
    public Project rename(UUID workspaceId, UUID projectId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("Project name must not be blank.");
        }

        UUID userId = currentUserProvider.getCurrentUserId();
        requireOwner(workspaceId, userId);

        Project project = projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Project not found."));

        String normalized = newName.strip();

        if (projectRepository.existsByWorkspaceIdAndNameAndIdNot(workspaceId, normalized, projectId)) {
            throw new ConflictException("Project with this name already exists in this workspace.");
        }

        project.rename(normalized);
        return project;
    }

    @Transactional
    public void archive(UUID workspaceId, UUID projectId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireOwner(workspaceId, userId);

        Project project = projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Project not found."));

        project.archive();
    }

    @Transactional
    public void restore(UUID workspaceId, UUID projectId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireOwner(workspaceId, userId);

        Project project = projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Project not found."));

        project.restore();
    }

    private void requireMember(UUID workspaceId, UUID userId) {
        workspaceMemberRepository.findRole(workspaceId, userId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));
    }

    private void requireOwner(UUID workspaceId, UUID userId) {
        WorkspaceMemberRole role = workspaceMemberRepository.findRole(workspaceId, userId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));

        if (role != WorkspaceMemberRole.OWNER) {
            throw new ForbiddenException("Only workspace owner can perform this action.");
        }
    }
}