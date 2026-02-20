package com.teamflow.teamflow.backend.workspaces.api;

import com.teamflow.teamflow.backend.common.api.PageResponse;
import com.teamflow.teamflow.backend.common.api.PageResponses;
import com.teamflow.teamflow.backend.workspaces.api.mapper.WorkspaceMapper;
import com.teamflow.teamflow.backend.workspaces.api.mapper.WorkspaceMemberMapper;
import com.teamflow.teamflow.backend.workspaces.domain.Workspace;
import com.teamflow.teamflow.backend.workspaces.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.web.SortDefault;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper workspaceMemberMapper;


    public WorkspaceController(WorkspaceService workspaceService, WorkspaceMapper workspaceMapper, WorkspaceMemberMapper workspaceMemberMapper) {
        this.workspaceService = workspaceService;
        this.workspaceMapper = workspaceMapper;
        this.workspaceMemberMapper = workspaceMemberMapper;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public WorkspaceResponse createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        Workspace ws = workspaceService.createWorkspace(request.name());
        return workspaceMapper.toResponse(ws);
    }

    @GetMapping("/{id}")
    public WorkspaceResponse getWorkspaceById(@PathVariable UUID id) {
        Workspace ws = workspaceService.getWorkspaceById(id);
        return workspaceMapper.toResponse(ws);
    }

    @GetMapping
    public PageResponse<WorkspaceResponse> getWorkspaces(
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var page = workspaceService.getWorkspaces(pageable);
        return PageResponses.of(page, workspaceMapper::toResponse);
    }

    @GetMapping("/closed")
    public PageResponse<WorkspaceResponse> getClosedWorkspaces(
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var page = workspaceService.getClosedWorkspaces(pageable);
        return PageResponses.of(page, workspaceMapper::toResponse);
    }

    @PatchMapping("/{id}")
    public WorkspaceResponse renameWorkspace(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        Workspace ws = workspaceService.renameWorkspace(id, request.name());
        return workspaceMapper.toResponse(ws);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{id}/close")
    public void closeWorkplace(@PathVariable UUID id) {
        workspaceService.closeWorkspace(id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{id}/restore")
    public void restoreWorkspace(@PathVariable UUID id) {
        workspaceService.restoreWorkspace(id);
    }

    @GetMapping("/{id}/members")
    public List<WorkspaceMemberResponse> getMembers(@PathVariable UUID id) {
        return workspaceService.getMembers(id)
                .stream()
                .map(workspaceMemberMapper::toResponse)
                .toList();
    }

    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveWorkspace(@PathVariable UUID id) {
        workspaceService.leaveWorkspace(id);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        workspaceService.removeMember(id, userId);
    }

    @PostMapping("/{id}/members/{userId}/promote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promoteMember(@PathVariable UUID id, @PathVariable UUID userId) {
        workspaceService.promoteMember(id, userId);
    }
}
