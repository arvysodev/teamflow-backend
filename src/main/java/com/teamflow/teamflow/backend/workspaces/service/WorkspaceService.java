package com.teamflow.teamflow.backend.workspaces.service;

import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.ConflictException;
import com.teamflow.teamflow.backend.common.errors.ForbiddenException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.common.security.CurrentUserProvider;
import com.teamflow.teamflow.backend.workspaces.domain.*;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceMemberRepository;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final CurrentUserProvider currentUserProvider;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            CurrentUserProvider currentUserProvider,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.currentUserProvider = currentUserProvider;
        this.workspaceMemberRepository = workspaceMemberRepository;
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

        Workspace saved = workspaceRepository.save(new Workspace(normalized));
        UUID userId = currentUserProvider.getCurrentUserId();
        WorkspaceMember member = WorkspaceMember.owner(saved.getId(), userId);

        workspaceMemberRepository.save(member);

        return saved;
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspaceById(UUID id) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return workspaceRepository.findByIdAndMember(id, userId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));
    }

    @Transactional(readOnly = true)
    public Page<Workspace> getWorkspaces(Pageable pageable) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return workspaceRepository.findAllByMemberAndStatus(userId, WorkspaceStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Workspace> getClosedWorkspaces(Pageable pageable) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return workspaceRepository.findAllByMemberAndStatus(userId, WorkspaceStatus.CLOSED, pageable);
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

        requireOwner(id);
        Workspace ws = getWorkspaceById(id);
        ws.rename(normalized);

        return ws;
    }

    @Transactional
    public void closeWorkspace(UUID id) {
        requireOwner(id);
        Workspace ws = getWorkspaceById(id);

        if (ws.getStatus() == WorkspaceStatus.CLOSED) {
            throw new ConflictException("Workspace is already closed.");
        }

        ws.close();
    }

    @Transactional
    public void restoreWorkspace(UUID id) {
        requireOwner(id);
        Workspace ws = getWorkspaceById(id);

        if (ws.getStatus() == WorkspaceStatus.ACTIVE) {
            throw new ConflictException("Workspace is already active.");
        }

        ws.restore();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMember> getMembers(UUID id) {
        getWorkspaceById(id);
        return workspaceMemberRepository.findByIdWorkspaceIdOrderByRoleAscJoinedAtAsc(id);
    }

    private void requireOwner(UUID workspaceId) {
        UUID userId = currentUserProvider.getCurrentUserId();

        var role = workspaceMemberRepository.findRole(workspaceId, userId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));

        if (role != WorkspaceMemberRole.OWNER) {
            throw new ForbiddenException("Only workspace owner can perform this action.");
        }
    }

    @Transactional
    public void leaveWorkspace(UUID workspaceId) {
        UUID userId = currentUserProvider.getCurrentUserId();

        WorkspaceMemberRole role = workspaceMemberRepository
                .findRole(workspaceId, userId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));

        if (role == WorkspaceMemberRole.OWNER) {
            long ownersCount = workspaceMemberRepository
                    .countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER);

            if (ownersCount <= 1) {
                throw new ConflictException("Cannot leave workspace as the only owner.");
            }
        }

        workspaceMemberRepository.deleteById(new WorkspaceMemberId(workspaceId, userId));
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID memberUserId) {
        UUID actorId = currentUserProvider.getCurrentUserId();

        WorkspaceMemberRole actorRole = workspaceMemberRepository.findRole(workspaceId, actorId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));

        if (actorRole != WorkspaceMemberRole.OWNER) {
            throw new ForbiddenException("Only workspace owner can perform this action.");
        }

        if (actorId.equals(memberUserId)) {
            throw new BadRequestException("Use leave endpoint to leave the workspace.");
        }

        boolean memberExists = workspaceMemberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, memberUserId);
        if (!memberExists) {
            throw new NotFoundException("Member not found.");
        }

        WorkspaceMemberRole targetRole = workspaceMemberRepository.findRole(workspaceId, memberUserId)
                .orElseThrow(() -> new NotFoundException("Member not found."));

        if (targetRole == WorkspaceMemberRole.OWNER) {
            long ownersCount = workspaceMemberRepository.countByIdWorkspaceIdAndRole(workspaceId, WorkspaceMemberRole.OWNER);
            if (ownersCount <= 1) {
                throw new ConflictException("Cannot remove the only owner.");
            }
        }

        workspaceMemberRepository.deleteById(new WorkspaceMemberId(workspaceId, memberUserId));
    }

    @Transactional
    public void promoteMember(UUID workspaceId, UUID memberUserId) {
        UUID actorId = currentUserProvider.getCurrentUserId();

        WorkspaceMemberRole actorRole = workspaceMemberRepository.findRole(workspaceId, actorId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));

        if (actorRole != WorkspaceMemberRole.OWNER) {
            throw new ForbiddenException("Only workspace owner can perform this action.");
        }

        WorkspaceMemberRole targetRole = workspaceMemberRepository.findRole(workspaceId, memberUserId)
                .orElseThrow(() -> new NotFoundException("Member not found."));

        if (targetRole == WorkspaceMemberRole.OWNER) {
            throw new ConflictException("Member is already an owner.");
        }

        workspaceMemberRepository.updateRole(workspaceId, memberUserId, WorkspaceMemberRole.OWNER);
    }
}
