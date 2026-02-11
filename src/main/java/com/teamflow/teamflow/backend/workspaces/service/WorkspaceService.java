package com.teamflow.teamflow.backend.workspaces.service;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.ConflictException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.workspaces.domain.Workspace;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceStatus;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
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

        return workspaceRepository.save(new Workspace(normalized));
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspaceById(UUID id) {
        return requireWorkspace(id);
    }

    @Transactional(readOnly = true)
    public Page<Workspace> getWorkspaces(Pageable pageable) {
        return workspaceRepository.findByStatus(WorkspaceStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Workspace> getClosedWorkspaces(Pageable pageable) {
        return workspaceRepository.findByStatus(WorkspaceStatus.CLOSED, pageable);
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
        Workspace ws = requireWorkspace(id);

        if (ws.getStatus() == WorkspaceStatus.CLOSED) {
            throw new ConflictException("Workspace is already closed.");
        }

        ws.close();
    }

    @Transactional
    public void restoreWorkspace(UUID id) {
        Workspace ws = requireWorkspace(id);

        if (ws.getStatus() == WorkspaceStatus.ACTIVE) {
            throw new ConflictException("Workspace is already active.");
        }

        ws.restore();
    }

    private Workspace requireWorkspace(UUID id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));
    }
}
