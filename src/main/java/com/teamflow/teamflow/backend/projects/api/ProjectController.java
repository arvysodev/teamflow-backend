package com.teamflow.teamflow.backend.projects.api;

import com.teamflow.teamflow.backend.common.api.PageResponse;
import com.teamflow.teamflow.backend.common.api.PageResponses;
import com.teamflow.teamflow.backend.projects.api.mapper.ProjectMapper;
import com.teamflow.teamflow.backend.projects.domain.Project;
import com.teamflow.teamflow.backend.projects.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;

    public ProjectController(ProjectService projectService, ProjectMapper projectMapper) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateProjectRequest req
    ) {
        Project created = projectService.create(workspaceId, req.name());
        return projectMapper.toResponse(created);
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        PageRequest pr = PageRequest.of(page, size);

        Page<Project> result = "ARCHIVED".equalsIgnoreCase(status)
                ? projectService.listArchived(workspaceId, pr)
                : projectService.listActive(workspaceId, pr);

        return PageResponses.of(result, projectMapper::toResponse);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        Project project = projectService.getById(workspaceId, id);
        return projectMapper.toResponse(project);
    }

    @PatchMapping("/{id}")
    public ProjectResponse rename(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id,
            @Valid @RequestBody RenameProjectRequest req
    ) {
        Project project = projectService.rename(workspaceId, id, req.name());
        return projectMapper.toResponse(project);
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        projectService.archive(workspaceId, id);
    }

    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable UUID workspaceId, @PathVariable UUID id) {
        projectService.restore(workspaceId, id);
    }
}