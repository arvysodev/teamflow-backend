package com.teamflow.teamflow.backend.tasks.service;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.common.security.CurrentUserProvider;
import com.teamflow.teamflow.backend.projects.repo.ProjectRepository;
import com.teamflow.teamflow.backend.tasks.domain.Task;
import com.teamflow.teamflow.backend.tasks.domain.TaskStatus;
import com.teamflow.teamflow.backend.tasks.repo.TaskRepository;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserProvider currentUserProvider;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public Task create(UUID workspaceId, UUID projectId, String title, String description) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);
        requireProjectInWorkspace(workspaceId, projectId);

        Task task = new Task(projectId, title, description, userId);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Page<Task> list(UUID workspaceId, UUID projectId, TaskStatus status, Pageable pageable) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);
        requireProjectInWorkspace(workspaceId, projectId);

        if (status == null) {
            return taskRepository.findAllByProjectId(projectId, pageable);
        }
        return taskRepository.findAllByProjectIdAndStatus(projectId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Task getById(UUID workspaceId, UUID projectId, UUID taskId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);
        requireProjectInWorkspace(workspaceId, projectId);

        return taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new NotFoundException("Task not found."));
    }

    @Transactional
    public Task update(UUID workspaceId, UUID projectId, UUID taskId, String title, String description) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);
        requireProjectInWorkspace(workspaceId, projectId);

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        task.updateDetails(title, description);
        return task;
    }

    @Transactional
    public Task changeStatus(UUID workspaceId, UUID projectId, UUID taskId, TaskStatus newStatus) {
        if (newStatus == null) {
            throw new BadRequestException("Task status must not be null.");
        }

        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);
        requireProjectInWorkspace(workspaceId, projectId);

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        task.changeStatus(newStatus);
        return task;
    }

    @Transactional
    public Task assign(UUID workspaceId, UUID projectId, UUID taskId, UUID assigneeUserId) {
        if (assigneeUserId == null) {
            throw new BadRequestException("Assignee userId must not be null.");
        }

        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);
        requireProjectInWorkspace(workspaceId, projectId);

        if (!workspaceMemberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, assigneeUserId)) {
            throw new BadRequestException("Assignee must be a workspace member.");
        }

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        task.assignTo(assigneeUserId);
        return task;
    }

    @Transactional
    public Task unassign(UUID workspaceId, UUID projectId, UUID taskId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        requireMember(workspaceId, userId);
        requireProjectInWorkspace(workspaceId, projectId);

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        task.unassign();
        return task;
    }

    private void requireMember(UUID workspaceId, UUID userId) {
        workspaceMemberRepository.findRole(workspaceId, userId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));
    }

    private void requireProjectInWorkspace(UUID workspaceId, UUID projectId) {
        if (projectRepository.findByIdAndWorkspaceId(projectId, workspaceId).isEmpty()) {
            throw new NotFoundException("Project not found.");
        }
    }
}
