package com.teamflow.teamflow.backend.workspaces.service;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.ConflictException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.common.security.CurrentUserProvider;
import com.teamflow.teamflow.backend.workspaces.domain.Workspace;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMember;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceStatus;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceMemberRepository;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final CurrentUserProvider currentUserProvider;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            CurrentUserProvider currentUserProvider,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.currentUserProvider = currentUserProvider;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public Workspace createWorkspace(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Workspace name must not be blank.");
        }

        String normalized = name.strip();

        if (workspaceRepository.existsByName(normalized)) {
            throw new ConflictException("Workspace with this name already exists.");
        }

        Workspace saved = workspaceRepository.save(new Workspace(normalized));
        UUID userId = currentUserProvider.getCurrentUserId();
        WorkspaceMember member = WorkspaceMember.owner(saved.getId(), userId);

        workspaceMemberRepository.save(member);

        return saved;
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspaceById(UUID id) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return workspaceRepository.findByIdAndMember(id, userId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));
    }

    @Transactional(readOnly = true)
    public Page<Workspace> getWorkspaces(Pageable pageable) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return workspaceRepository.findAllByMemberAndStatus(userId, WorkspaceStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Workspace> getClosedWorkspaces(Pageable pageable) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return workspaceRepository.findAllByMemberAndStatus(userId, WorkspaceStatus.CLOSED, pageable);
    }

    @Transactional
    public Workspace renameWorkspace(UUID id, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("Workspace name must not be blank.");
        }

        String normalized = newName.strip();

        if (workspaceRepository.existsByNameAndIdNot(normalized, id)) {
            throw new ConflictException("Workspace with this name already exists.");
        }

        Workspace ws = getWorkspaceById(id);
        ws.rename(normalized);

        return ws;
    }

    @Transactional
    public void closeWorkspace(UUID id) {
        Workspace ws = getWorkspaceById(id);

        if (ws.getStatus() == WorkspaceStatus.CLOSED) {
            throw new ConflictException("Workspace is already closed.");
        }

        ws.close();
    }

    @Transactional
    public void restoreWorkspace(UUID id) {
        Workspace ws = getWorkspaceById(id);

        if (ws.getStatus() == WorkspaceStatus.ACTIVE) {
            throw new ConflictException("Workspace is already active.");
        }

        ws.restore();
    }
}
