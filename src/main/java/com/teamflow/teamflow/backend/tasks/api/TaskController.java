package com.teamflow.teamflow.backend.tasks.api;

import com.teamflow.teamflow.backend.common.api.PageResponse;
import com.teamflow.teamflow.backend.common.api.PageResponses;
import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.tasks.api.mapper.TaskMapper;
import com.teamflow.teamflow.backend.tasks.domain.Task;
import com.teamflow.teamflow.backend.tasks.domain.TaskStatus;
import com.teamflow.teamflow.backend.tasks.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest req
    ) {
        Task created = taskService.create(workspaceId, projectId, req.title(), req.description());
        return taskMapper.toResponse(created);
    }

    @GetMapping
    public PageResponse<TaskResponse> list(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status
    ) {
        PageRequest pr = PageRequest.of(page, size);
        TaskStatus parsed = (status == null || status.isBlank()) ? null : parseStatus(status);

        Page<Task> result = taskService.list(workspaceId, projectId, parsed, pr);
        return PageResponses.of(result, taskMapper::toResponse);
    }

    @GetMapping("/{id}")
    public TaskResponse get(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @PathVariable UUID id
    ) {
        Task task = taskService.getById(workspaceId, projectId, id);
        return taskMapper.toResponse(task);
    }

    @PatchMapping("/{id}")
    public TaskResponse update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest req
    ) {
        Task updated = taskService.update(workspaceId, projectId, id, req.title(), req.description());
        return taskMapper.toResponse(updated);
    }

    @PostMapping("/{id}/status")
    public TaskResponse changeStatus(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody ChangeTaskStatusRequest req
    ) {
        TaskStatus newStatus = parseStatus(req.status());
        Task task = taskService.changeStatus(workspaceId, projectId, id, newStatus);
        return taskMapper.toResponse(task);
    }

    @PostMapping("/{id}/assign")
    public TaskResponse assign(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody AssignTaskRequest req
    ) {
        Task task = taskService.assign(workspaceId, projectId, id, req.userId());
        return taskMapper.toResponse(task);
    }

    @PostMapping("/{id}/unassign")
    public TaskResponse unassign(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            @PathVariable UUID id
    ) {
        Task task = taskService.unassign(workspaceId, projectId, id);
        return taskMapper.toResponse(task);
    }

    private TaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Task status must not be blank.");
        }
        try {
            return TaskStatus.valueOf(status.strip().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid task status.");
        }
    }
}
