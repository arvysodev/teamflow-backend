package com.teamflow.teamflow.backend.workspaces.service;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.workspaces.domain.Workspace;
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

        Workspace workspace = new Workspace(name);
        return workspaceRepository.save(workspace);
    }

    public Workspace getWorkspaceById(UUID id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));
    }

    public Page<Workspace> getWorkspaces(Pageable pageable) {
        return workspaceRepository.findAll(pageable);
    }
}
