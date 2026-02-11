package com.teamflow.teamflow.backend.workspaces.api;

import com.teamflow.teamflow.backend.workspaces.api.mapper.WorkspaceMapper;
import com.teamflow.teamflow.backend.workspaces.domain.Workspace;
import com.teamflow.teamflow.backend.workspaces.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceMapper workspaceMapper;


    public WorkspaceController(WorkspaceService workspaceService, WorkspaceMapper workspaceMapper) {
        this.workspaceService = workspaceService;
        this.workspaceMapper = workspaceMapper;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public WorkspaceResponse createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        Workspace ws = workspaceService.createWorkspace(request.name());
        return workspaceMapper.toResponse(ws);
    }

    @GetMapping("/{id}")
    public WorkspaceResponse getWorkspaceById(UUID id) {
        Workspace ws = workspaceService.getWorkspaceById(id);
        return workspaceMapper.toResponse(ws);
    }

    @GetMapping
    public Page<WorkspaceResponse> getWorkspaces(Pageable pageable) {
        return workspaceService
                .getWorkspaces(pageable)
                .map(workspaceMapper::toResponse);
    }
}
