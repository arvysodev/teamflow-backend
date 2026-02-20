package com.teamflow.teamflow.backend.tasks.service;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.common.security.CurrentUserProvider;
import com.teamflow.teamflow.backend.projects.domain.Project;
import com.teamflow.teamflow.backend.projects.repo.ProjectRepository;
import com.teamflow.teamflow.backend.tasks.domain.Task;
import com.teamflow.teamflow.backend.tasks.domain.TaskStatus;
import com.teamflow.teamflow.backend.tasks.repo.TaskRepository;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberRole;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserProvider currentUserProvider;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        projectRepository = mock(ProjectRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);

        taskService = new TaskService(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void create_whenValid_shouldSaveAndReturnTask() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Task result = taskService.create(workspaceId, projectId, "  Title  ", "  Desc  ");

        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals("Title", result.getTitle());
        assertEquals("Desc", result.getDescription());
        assertEquals(TaskStatus.TODO, result.getStatus());
        assertEquals(userId, result.getCreatedBy());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(taskRepository).save(any(Task.class));
        verifyNoMoreInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void create_whenNotMember_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> taskService.create(workspaceId, projectId, "Title", null)
        );

        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
        verifyNoInteractions(projectRepository, taskRepository);
    }

    @Test
    void create_whenProjectNotFound_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> taskService.create(workspaceId, projectId, "Title", null)
        );

        assertEquals("Project not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void create_whenTitleBlank_shouldThrowBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> taskService.create(workspaceId, projectId, "   ", "Desc")
        );

        assertEquals("Task title must not be blank.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void list_whenMemberAndStatusNull_shouldReturnPage() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Task t1 = new Task(projectId, "A", null, userId);
        Task t2 = new Task(projectId, "B", null, userId);
        Page<Task> repoPage = new PageImpl<>(List.of(t1, t2), pageable, 2);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));
        when(taskRepository.findAllByProjectId(projectId, pageable))
                .thenReturn(repoPage);

        Page<Task> result = taskService.list(workspaceId, projectId, null, pageable);

        assertSame(repoPage, result);
        assertEquals(2, result.getTotalElements());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(taskRepository).findAllByProjectId(projectId, pageable);
        verifyNoMoreInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void list_whenMemberAndStatusProvided_shouldReturnPage() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Task t1 = new Task(projectId, "A", null, userId);
        t1.changeStatus(TaskStatus.IN_PROGRESS);
        Page<Task> repoPage = new PageImpl<>(List.of(t1), pageable, 1);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));
        when(taskRepository.findAllByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS, pageable))
                .thenReturn(repoPage);

        Page<Task> result = taskService.list(workspaceId, projectId, TaskStatus.IN_PROGRESS, pageable);

        assertSame(repoPage, result);
        assertEquals(1, result.getTotalElements());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(taskRepository).findAllByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS, pageable);
        verifyNoMoreInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void getById_whenTaskExists_shouldReturnTask() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Task task = new Task(projectId, "Title", null, userId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));
        when(taskRepository.findByIdAndProjectId(taskId, projectId))
                .thenReturn(Optional.of(task));

        Task result = taskService.getById(workspaceId, projectId, taskId);

        assertSame(task, result);

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(taskRepository).findByIdAndProjectId(taskId, projectId);
        verifyNoMoreInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void getById_whenTaskMissing_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));
        when(taskRepository.findByIdAndProjectId(taskId, projectId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> taskService.getById(workspaceId, projectId, taskId)
        );

        assertEquals("Task not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(taskRepository).findByIdAndProjectId(taskId, projectId);
        verifyNoMoreInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void update_whenValid_shouldUpdateAndReturnTask() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Task task = new Task(projectId, "Old", "OldDesc", userId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));
        when(taskRepository.findByIdAndProjectId(taskId, projectId))
                .thenReturn(Optional.of(task));

        Task result = taskService.update(workspaceId, projectId, taskId, " New ", "  NewDesc  ");

        assertSame(task, result);
        assertEquals("New", task.getTitle());
        assertEquals("NewDesc", task.getDescription());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(taskRepository).findByIdAndProjectId(taskId, projectId);
        verifyNoMoreInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void update_whenTitleBlank_shouldThrowBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Task task = new Task(projectId, "Old", null, userId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));
        when(taskRepository.findByIdAndProjectId(taskId, projectId))
                .thenReturn(Optional.of(task));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> taskService.update(workspaceId, projectId, taskId, "   ", "Desc")
        );

        assertEquals("Task title must not be blank.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(taskRepository).findByIdAndProjectId(taskId, projectId);
        verifyNoMoreInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void changeStatus_whenNullStatus_shouldThrowBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> taskService.changeStatus(workspaceId, projectId, taskId, null)
        );

        assertEquals("Task status must not be null.", exception.getMessage());

        verifyNoInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void assign_whenAssigneeNull_shouldThrowBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> taskService.assign(workspaceId, projectId, taskId, null)
        );

        assertEquals("Assignee userId must not be null.", exception.getMessage());

        verifyNoInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void assign_whenAssigneeNotWorkspaceMember_shouldThrowBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));
        when(workspaceMemberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, assigneeId))
                .thenReturn(false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> taskService.assign(workspaceId, projectId, taskId, assigneeId)
        );

        assertEquals("Assignee must be a workspace member.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(workspaceMemberRepository).existsByIdWorkspaceIdAndIdUserId(workspaceId, assigneeId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void unassign_whenTaskExists_shouldUnassign() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        Task task = new Task(projectId, "Title", null, userId);
        task.assignTo(assigneeId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(mock(Project.class)));
        when(taskRepository.findByIdAndProjectId(taskId, projectId))
                .thenReturn(Optional.of(task));

        Task result = taskService.unassign(workspaceId, projectId, taskId);

        assertSame(task, result);
        assertNull(task.getAssigneeUserId());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(taskRepository).findByIdAndProjectId(taskId, projectId);
        verifyNoMoreInteractions(taskRepository, projectRepository, workspaceMemberRepository, currentUserProvider);
    }
}
