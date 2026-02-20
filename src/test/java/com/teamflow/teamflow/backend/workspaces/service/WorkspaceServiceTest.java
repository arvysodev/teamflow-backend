package com.teamflow.teamflow.backend.workspaces.service;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.ConflictException;
import com.teamflow.teamflow.backend.common.errors.ForbiddenException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.common.security.CurrentUserProvider;
import com.teamflow.teamflow.backend.workspaces.domain.Workspace;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberId;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberRole;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceStatus;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceMemberRepository;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceRepository;
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

public class WorkspaceServiceTest {
    private WorkspaceRepository workspaceRepository;
    private WorkspaceService workspaceService;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);

        workspaceService = new WorkspaceService(workspaceRepository, currentUserProvider, workspaceMemberRepository);

    }

    @Test
    void createWorkspace_validName_savesAndReturnsWorkspace() {
        String inputName = " Test ";
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        when(workspaceRepository.existsByName("Test")).thenReturn(false);

        when(workspaceRepository.save(any(Workspace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Workspace result = workspaceService.createWorkspace(inputName);

        assertNotNull(result);
        assertEquals("Test", result.getName());

        verify(workspaceRepository).existsByName("Test");
        verify(workspaceRepository).save(any(Workspace.class));
        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).save(any());
        verifyNoMoreInteractions(workspaceRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void createWorkspace_whenNameBlank_shouldThrowBadRequest() {
        String inputName = "";

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> workspaceService.createWorkspace(inputName)
        );
        assertEquals("Workspace name must not be blank.", exception.getMessage());

        verifyNoInteractions(workspaceRepository, workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void createWorkspace_whenNameAlreadyExists_shouldThrowConflict() {
        String inputName = " Test ";

        when(workspaceRepository.existsByName("Test")).thenReturn(true);
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> workspaceService.createWorkspace(inputName)
        );
        assertEquals("Workspace with this name already exists.", exception.getMessage());

        verify(workspaceRepository).existsByName("Test");
        verifyNoMoreInteractions(workspaceRepository);
        verifyNoInteractions(currentUserProvider, workspaceMemberRepository);
    }

    @Test
    void getWorkspaces_shouldReturnActivePageFromRepository() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Workspace ws1 = new Workspace("A");
        Workspace ws2 = new Workspace("B");
        Page<Workspace> repoPage = new PageImpl<>(List.of(ws1, ws2), pageable, 2);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        when(workspaceRepository.findAllByMemberAndStatus(userId, WorkspaceStatus.ACTIVE, pageable))
                .thenReturn(repoPage);

        Page<Workspace> result = workspaceService.getWorkspaces(pageable);

        assertSame(repoPage, result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceRepository).findAllByMemberAndStatus(userId, WorkspaceStatus.ACTIVE, pageable);
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void getClosedWorkspaces_shouldReturnClosedPageFromRepository() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Workspace ws1 = new Workspace("A");
        Workspace ws2 = new Workspace("B");
        ws1.close();
        ws2.close();
        Page<Workspace> repoPage = new PageImpl<>(List.of(ws1, ws2), pageable, 2);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        when(workspaceRepository.findAllByMemberAndStatus(userId, WorkspaceStatus.CLOSED, pageable))
                .thenReturn(repoPage);

        Page<Workspace> result = workspaceService.getClosedWorkspaces(pageable);

        assertSame(repoPage, result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceRepository).findAllByMemberAndStatus(userId, WorkspaceStatus.CLOSED, pageable);
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void getWorkspaceById_whenExists_shouldReturnWorkspace() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Workspace ws = new Workspace("Test");

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.of(ws));

        Workspace found = workspaceService.getWorkspaceById(id);

        assertSame(ws, found);
        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceRepository).findByIdAndMember(id, userId);
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void getWorkspaceById_whenMissing_shouldThrowNotFound() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> workspaceService.getWorkspaceById(id)
        );
        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceRepository).findByIdAndMember(id, userId);
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void renameWorkspace_shouldSucceed() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Workspace ws = new Workspace("Before");
        String newName = "  After  ";

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(id, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.of(ws));
        when(workspaceRepository.existsByNameAndIdNot("After", id)).thenReturn(false);

        Workspace result = workspaceService.renameWorkspace(id, newName);

        assertSame(ws, result);
        assertEquals("After", result.getName());

        verify(currentUserProvider, times(2)).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(id, userId);
        verify(workspaceRepository).findByIdAndMember(id, userId);
        verify(workspaceRepository).existsByNameAndIdNot("After", id);
        verify(workspaceRepository, never()).save(any());
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void renameWorkspace_whenBlankNewName_shouldThrowBadRequest() {
        UUID id = UUID.randomUUID();
        String newName = "";

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> workspaceService.renameWorkspace(id, newName)
        );

        assertEquals("Workspace name must not be blank.", exception.getMessage());

        verifyNoInteractions(workspaceRepository);
    }

    @Test
    void renameWorkspace_whenNameAlreadyExists_shouldThrowConflict() {
        UUID id = UUID.randomUUID();
        String newName = " Test ";

        when(workspaceRepository.existsByNameAndIdNot("Test", id)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> workspaceService.renameWorkspace(id, newName)
        );

        assertEquals("Workspace with this name already exists.", exception.getMessage());

        verify(workspaceRepository).existsByNameAndIdNot("Test", id);
        verifyNoMoreInteractions(workspaceRepository);
    }

    @Test
    void renameWorkspace_whenWorkspaceClosed_shouldThrowConflict() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Workspace ws = new Workspace("Before");
        ws.close();
        String newName = " After ";

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(id, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.of(ws));
        when(workspaceRepository.existsByNameAndIdNot("After", id)).thenReturn(false);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> workspaceService.renameWorkspace(id, newName)
        );

        assertEquals("Closed workspace cannot be renamed.", exception.getMessage());

        verify(currentUserProvider, times(2)).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(id, userId);
        verify(workspaceRepository).existsByNameAndIdNot("After", id);
        verify(workspaceRepository).findByIdAndMember(id, userId);
        verify(workspaceRepository, never()).save(any());
        verifyNoMoreInteractions(workspaceRepository,currentUserProvider);
    }

    @Test
    void renameWorkspace_whenWorkspaceNotFound_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String newName = "Test";

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        when(workspaceRepository.existsByNameAndIdNot(newName, id)).thenReturn(false);
        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> workspaceService.renameWorkspace(id, newName)
        );

        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceRepository, never()).save(any());
        verify(workspaceRepository).existsByNameAndIdNot(newName, id);
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void closeWorkspace_shouldSucceed() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Workspace ws = new Workspace("Test");

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(id, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.of(ws));

        workspaceService.closeWorkspace(id);
        assertEquals(WorkspaceStatus.CLOSED, ws.getStatus());

        verify(currentUserProvider, times(2)).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(id, userId);
        verify(workspaceRepository).findByIdAndMember(id, userId);
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void closeWorkspace_whenAlreadyClosed_shouldThrowConflict() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Workspace ws = new Workspace("Test");
        ws.close();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(id, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.of(ws));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> workspaceService.closeWorkspace(id)
        );

        assertEquals("Workspace is already closed.", exception.getMessage());

        verify(currentUserProvider, times(2)).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(id, userId);
        verify(workspaceRepository).findByIdAndMember(id ,userId);
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void closeWorkspace_whenWorkspaceNotFound_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> workspaceService.closeWorkspace(id)
        );

        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verifyNoMoreInteractions(workspaceRepository);
    }

    @Test
    void restoreWorkspace_shouldSucceed() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Workspace ws = new Workspace("Test");
        ws.close();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(id, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.of(ws));

        workspaceService.restoreWorkspace(id);
        assertEquals(WorkspaceStatus.ACTIVE, ws.getStatus());

        verify(currentUserProvider, times(2)).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(id, userId);
        verify(workspaceRepository).findByIdAndMember(id, userId);
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void restoreWorkspace_whenAlreadyActive_shouldThrowConflict() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Workspace ws = new Workspace("Test");
        ws.restore();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(id, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.of(ws));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> workspaceService.restoreWorkspace(id)
        );

        assertEquals("Workspace is already active.", exception.getMessage());

        verify(currentUserProvider, times(2)).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(id, userId);
        verify(workspaceRepository).findByIdAndMember(id, userId);
        verifyNoMoreInteractions(workspaceRepository, currentUserProvider);
    }

    @Test
    void restoreWorkspace_whenWorkspaceNotFound_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

        when(workspaceRepository.findByIdAndMember(id, userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> workspaceService.restoreWorkspace(id)
        );

        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verifyNoMoreInteractions(workspaceRepository);
    }

    @Test
    void leaveWorkspace_whenMember_shouldDeleteMembership() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));

        workspaceService.leaveWorkspace(workspaceId);

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(workspaceMemberRepository)
                .deleteById(new WorkspaceMemberId(workspaceId, userId));
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void leaveWorkspace_whenOwnerAndMultipleOwners_shouldDeleteMembership() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository
                .countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER))
                .thenReturn(2L);

        workspaceService.leaveWorkspace(workspaceId);

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(workspaceMemberRepository)
                .countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER);
        verify(workspaceMemberRepository)
                .deleteById(new WorkspaceMemberId(workspaceId, userId));
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void leaveWorkspace_whenOwnerAndSingleOwner_shouldThrowConflict() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository
                .countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER))
                .thenReturn(1L);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> workspaceService.leaveWorkspace(workspaceId)
        );

        assertEquals("Cannot leave workspace as the only owner.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);
        verify(workspaceMemberRepository)
                .countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER);

        verify(workspaceMemberRepository, never())
                .deleteById(any());

        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void leaveWorkspace_whenNotMember_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(workspaceMemberRepository.findRole(workspaceId, userId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> workspaceService.leaveWorkspace(workspaceId)
        );

        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, userId);

        verify(workspaceMemberRepository, never())
                .deleteById(any());

        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void removeMember_whenOwnerRemovesMember_shouldSucceed() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, memberId))
                .thenReturn(true);
        when(workspaceMemberRepository.findRole(workspaceId, memberId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));

        workspaceService.removeMember(workspaceId, memberId);

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(workspaceMemberRepository).existsByIdWorkspaceIdAndIdUserId(workspaceId, memberId);
        verify(workspaceMemberRepository).findRole(workspaceId, memberId);
        verify(workspaceMemberRepository).deleteById(new WorkspaceMemberId(workspaceId, memberId));
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void removeMember_whenActorNotMember_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(actorId);
        when(workspaceMemberRepository.findRole(workspaceId, actorId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> workspaceService.removeMember(workspaceId, memberId)
        );

        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, actorId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void removeMember_whenActorIsMemberButNotOwner_shouldThrowForbidden() {
        UUID workspaceId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(actorId);
        when(workspaceMemberRepository.findRole(workspaceId, actorId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> workspaceService.removeMember(workspaceId, memberId)
        );

        assertEquals("Only workspace owner can perform this action.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, actorId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void removeMember_whenOwnerRemovesSelf_shouldThrowBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> workspaceService.removeMember(workspaceId, ownerId)
        );

        assertEquals("Use leave endpoint to leave the workspace.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void removeMember_whenTargetNotMember_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, memberId))
                .thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> workspaceService.removeMember(workspaceId, memberId)
        );

        assertEquals("Member not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(workspaceMemberRepository).existsByIdWorkspaceIdAndIdUserId(workspaceId, memberId);
        verify(workspaceMemberRepository, never()).deleteById(any());
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void removeMember_whenRemovingOnlyOwner_shouldThrowConflict() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID targetOwnerId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, targetOwnerId))
                .thenReturn(true);
        when(workspaceMemberRepository.findRole(workspaceId, targetOwnerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository.countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER))
                .thenReturn(1L);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> workspaceService.removeMember(workspaceId, targetOwnerId)
        );

        assertEquals("Cannot remove the only owner.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(workspaceMemberRepository).existsByIdWorkspaceIdAndIdUserId(workspaceId, targetOwnerId);
        verify(workspaceMemberRepository).findRole(workspaceId, targetOwnerId);
        verify(workspaceMemberRepository).countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER);
        verify(workspaceMemberRepository, never()).deleteById(any());
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void removeMember_whenTargetOwnerAndMultipleOwners_shouldDeleteMembership() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID targetOwnerId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, targetOwnerId))
                .thenReturn(true);
        when(workspaceMemberRepository.findRole(workspaceId, targetOwnerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository.countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER))
                .thenReturn(2L);

        workspaceService.removeMember(workspaceId, targetOwnerId);

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(workspaceMemberRepository).existsByIdWorkspaceIdAndIdUserId(workspaceId, targetOwnerId);
        verify(workspaceMemberRepository).findRole(workspaceId, targetOwnerId);
        verify(workspaceMemberRepository).countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER);
        verify(workspaceMemberRepository).deleteById(new WorkspaceMemberId(workspaceId, targetOwnerId));
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void promoteMember_whenOwnerPromotesMember_shouldSucceed() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository.findRole(workspaceId, memberId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));

        when(workspaceMemberRepository.updateRole(workspaceId, memberId, WorkspaceMemberRole.OWNER))
                .thenReturn(1);

        workspaceService.promoteMember(workspaceId, memberId);

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(workspaceMemberRepository).findRole(workspaceId, memberId);
        verify(workspaceMemberRepository).updateRole(workspaceId, memberId, WorkspaceMemberRole.OWNER);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void promoteMember_whenActorNotMember_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(actorId);
        when(workspaceMemberRepository.findRole(workspaceId, actorId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> workspaceService.promoteMember(workspaceId, memberId)
        );

        assertEquals("Workspace not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, actorId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void promoteMember_whenActorIsMemberButNotOwner_shouldThrowForbidden() {
        UUID workspaceId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(actorId);
        when(workspaceMemberRepository.findRole(workspaceId, actorId))
                .thenReturn(Optional.of(WorkspaceMemberRole.MEMBER));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> workspaceService.promoteMember(workspaceId, memberId)
        );

        assertEquals("Only workspace owner can perform this action.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, actorId);
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void promoteMember_whenTargetNotMember_shouldThrowNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository.findRole(workspaceId, memberId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> workspaceService.promoteMember(workspaceId, memberId)
        );

        assertEquals("Member not found.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(workspaceMemberRepository).findRole(workspaceId, memberId);
        verify(workspaceMemberRepository, never()).updateRole(any(), any(), any());
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }

    @Test
    void promoteMember_whenTargetAlreadyOwner_shouldThrowConflict() {
        UUID workspaceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID targetOwnerId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUserId()).thenReturn(ownerId);
        when(workspaceMemberRepository.findRole(workspaceId, ownerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        when(workspaceMemberRepository.findRole(workspaceId, targetOwnerId))
                .thenReturn(Optional.of(WorkspaceMemberRole.OWNER));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> workspaceService.promoteMember(workspaceId, targetOwnerId)
        );

        assertEquals("Member is already an owner.", exception.getMessage());

        verify(currentUserProvider).getCurrentUserId();
        verify(workspaceMemberRepository).findRole(workspaceId, ownerId);
        verify(workspaceMemberRepository).findRole(workspaceId, targetOwnerId);
        verify(workspaceMemberRepository, never()).updateRole(any(), any(), any());
        verifyNoMoreInteractions(workspaceMemberRepository, currentUserProvider);
    }
}
