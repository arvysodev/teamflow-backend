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

class ProjectServiceTest {

    private ProjectRepository projectRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserProvider currentUserProvider;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);

        projectService = new ProjectService(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void create_whenValid_shouldSaveAndReturnProject() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String inputName = "  Project  ";

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.existsByWorkspaceIdAndName(workspaceId, "Project"))
                .thenReturn(false);
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project result = projectService.create(workspaceId, inputName);

        assertNotNull(result);
        assertEquals(workspaceId, result.getWorkspaceId());
        assertEquals("Project", result.getName());
        assertEquals(ProjectStatus.ACTIVE, result.getStatus());
        assertEquals(userId, result.getCreatedBy());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).existsByWorkspaceIdAndName(workspaceId, "Project");
        verify(projectRepository).save(any(Project.class));
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void create_whenNameBlank_shouldThrowBadRequest() {
        UUID workspaceId = UUID.randomUUID();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> projectService.create(workspaceId, "   ")
        );

        assertEquals("Project name must not be blank.", exception.getMessage());

        verifyNoInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void create_whenNotMember_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> projectService.create(workspaceId, "Project")
        );

        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
        verifyNoInteractions(projectRepository);
    }

    @Test
    void create_whenNameAlreadyExists_shouldThrowConflict() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.existsByWorkspaceIdAndName(workspaceId, "Project"))
                .thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> projectService.create(workspaceId, " Project ")
        );

        assertEquals("Project with this name already exists in this workspace.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).existsByWorkspaceIdAndName(workspaceId, "Project");
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void listActive_whenMember_shouldReturnPage() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Project p1 = new Project(workspaceId, "A", userId);
        Project p2 = new Project(workspaceId, "B", userId);
        Page<Project> repoPage = new PageImpl<>(List.of(p1, p2), pageable, 2);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findAllByWorkspaceIdAndStatus(workspaceId, ProjectStatus.ACTIVE, pageable))
                .thenReturn(repoPage);

        Page<Project> result = projectService.listActive(workspaceId, pageable);

        assertSame(repoPage, result);
        assertEquals(2, result.getTotalElements());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findAllByWorkspaceIdAndStatus(workspaceId, ProjectStatus.ACTIVE, pageable);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void listActive_whenNotMember_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> projectService.listActive(workspaceId, pageable)
        );

        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
        verifyNoInteractions(projectRepository);
    }

    @Test
    void listArchived_whenMember_shouldReturnPage() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Project p1 = new Project(workspaceId, "A", userId);
        Project p2 = new Project(workspaceId, "B", userId);
        Page<Project> repoPage = new PageImpl<>(List.of(p1, p2), pageable, 2);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findAllByWorkspaceIdAndStatus(workspaceId, ProjectStatus.ARCHIVED, pageable))
                .thenReturn(repoPage);

        Page<Project> result = projectService.listArchived(workspaceId, pageable);

        assertSame(repoPage, result);
        assertEquals(2, result.getTotalElements());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findAllByWorkspaceIdAndStatus(workspaceId, ProjectStatus.ARCHIVED, pageable);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void getById_whenMemberAndProjectExists_shouldReturnProject() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Project project = new Project(workspaceId, "A", userId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(project));

        Project result = projectService.getById(workspaceId, projectId);

        assertSame(project, result);

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void getById_whenMemberButProjectMissing_shouldThrowNotFound() {
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
                () -> projectService.getById(workspaceId, projectId)
        );

        assertEquals("Project not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void rename_whenNameBlank_shouldThrowBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> projectService.rename(workspaceId, projectId, "   ")
        );

        assertEquals("Project name must not be blank.", exception.getMessage());

        verifyNoInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void rename_whenNotOwner_shouldThrowForbidden() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> projectService.rename(workspaceId, projectId, "New")
        );

        assertEquals("Only workspace owner can perform this action.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
        verifyNoInteractions(projectRepository);
    }

    @Test
    void rename_whenProjectMissing_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> projectService.rename(workspaceId, projectId, "New")
        );

        assertEquals("Project not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(projectRepository, never()).existsByWorkspaceIdAndNameAndIdNot(any(), any(), any());
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void rename_whenNameTaken_shouldThrowConflict() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = new Project(workspaceId, "Old", ownerId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(project));
        when(projectRepository.existsByWorkspaceIdAndNameAndIdNot(workspaceId, "New", projectId))
                .thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> projectService.rename(workspaceId, projectId, " New ")
        );

        assertEquals("Project with this name already exists in this workspace.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(projectRepository).existsByWorkspaceIdAndNameAndIdNot(workspaceId, "New", projectId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void rename_whenValid_shouldRenameAndReturnProject() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = new Project(workspaceId, "Old", ownerId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(project));
        when(projectRepository.existsByWorkspaceIdAndNameAndIdNot(workspaceId, "New", projectId))
                .thenReturn(false);

        Project result = projectService.rename(workspaceId, projectId, " New ");

        assertSame(project, result);
        assertEquals("New", project.getName());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verify(projectRepository).existsByWorkspaceIdAndNameAndIdNot(workspaceId, "New", projectId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void archive_whenNotOwner_shouldThrowForbidden() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> projectService.archive(workspaceId, projectId)
        );

        assertEquals("Only workspace owner can perform this action.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
        verifyNoInteractions(projectRepository);
    }

    @Test
    void archive_whenProjectMissing_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> projectService.archive(workspaceId, projectId)
        );

        assertEquals("Project not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void archive_whenValid_shouldArchive() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = new Project(workspaceId, "A", ownerId);

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(project));

        projectService.archive(workspaceId, projectId);

        assertEquals(ProjectStatus.ARCHIVED, project.getStatus());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void restore_whenNotOwner_shouldThrowForbidden() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> projectService.restore(workspaceId, projectId)
        );

        assertEquals("Only workspace owner can perform this action.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
        verifyNoInteractions(projectRepository);
    }

    @Test
    void restore_whenValid_shouldRestore() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Project project = new Project(workspaceId, "A", ownerId);
        project.archive();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));
        when(projectRepository.findByIdAndWorkspaceId(projectId, workspaceId))
                .thenReturn(Optional.of(project));

        projectService.restore(workspaceId, projectId);

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(projectRepository).findByIdAndWorkspaceId(projectId, workspaceId);
        verifyNoMoreInteractions(projectRepository, workspaceMemberRepository, currentUserProvider);
    }
}