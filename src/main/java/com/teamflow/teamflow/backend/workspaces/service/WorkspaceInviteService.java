package com.teamflow.teamflow.backend.workspaces.service;

import com.teamflow.teamflow.backend.auth.notify.VerificationNotifier;
import com.teamflow.teamflow.backend.auth.security.EmailVerificationTokenGenerator;
import com.teamflow.teamflow.backend.auth.security.TokenHasher;
import com.teamflow.teamflow.backend.common.errors.ConflictException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.ForbiddenException;
import com.teamflow.teamflow.backend.common.security.CurrentUserProvider;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceInvite;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMember;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberRole;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceInviteRepository;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkspaceInviteService {

    private static final int INVITE_TTL_HOURS = 48;

    private final CurrentUserProvider currentUserProvider;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInviteRepository workspaceInviteRepository;

    private final EmailVerificationTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final VerificationNotifier notifier;

    public WorkspaceInviteService(
            CurrentUserProvider currentUserProvider,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceInviteRepository workspaceInviteRepository,
            EmailVerificationTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            VerificationNotifier notifier
    ) {
        this.currentUserProvider = currentUserProvider;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceInviteRepository = workspaceInviteRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.notifier = notifier;
    }

    @Transactional
    public void invite(UUID workspaceId, String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email must not be blank.");
        }

        String normalizedEmail = email.toLowerCase().strip();
        UUID inviterId = currentUserProvider.getCurrentUserId();

        WorkspaceMemberRole role = workspaceMemberRepository.findRole(workspaceId, inviterId)
                .orElseThrow(() -> new NotFoundException("Workspace not found."));

        if (role != WorkspaceMemberRole.OWNER) {
            throw new ForbiddenException("Only workspace owner can invite members.");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean activeInviteExists = workspaceInviteRepository
                .existsByWorkspaceIdAndEmailAndAcceptedAtIsNullAndExpiresAtAfter(workspaceId, normalizedEmail, now);

        if (activeInviteExists) {
            throw new ConflictException("Active invite already exists for this email.");
        }

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHasher.sha256Base64Url(rawToken);

        LocalDateTime expiresAt = now.plusHours(INVITE_TTL_HOURS);

        WorkspaceInvite invite = new WorkspaceInvite(
                workspaceId,
                normalizedEmail,
                tokenHash,
                expiresAt,
                inviterId
        );

        workspaceInviteRepository.save(invite);

        notifier.sendWorkspaceInvite(normalizedEmail, rawToken, workspaceId, expiresAt);
    }

    @Transactional
    public void acceptInvite(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Invite token must not be blank.");
        }

        String tokenHash = tokenHasher.sha256Base64Url(rawToken);
        UUID userId = currentUserProvider.getCurrentUserId();
        String userEmail = currentUserProvider.getCurrentUserEmail().toLowerCase().strip();
        LocalDateTime now = LocalDateTime.now();

        WorkspaceInvite invite = workspaceInviteRepository.findTokenByHash(tokenHash)
                .orElseThrow(() -> new NotFoundException("Invite token is invalid."));

        if (invite.getAcceptedAt() != null) {
            throw new ConflictException("Invite is already accepted.");
        }

        if (now.isAfter(invite.getExpiresAt())) {
            throw new BadRequestException("Invite token has expired.");
        }

        if (!userEmail.equals(invite.getEmail().toLowerCase().strip())) {
            throw new ForbiddenException("This invite was issued for a different email.");
        }

        if (workspaceMemberRepository.existsByIdWorkspaceIdAndIdUserId(invite.getWorkspaceId(),
                userId)) {
            throw new ConflictException("User is already a member.");
        }

        workspaceMemberRepository.save(WorkspaceMember.member(invite.getWorkspaceId(), userId));
        invite.accept(now);
        workspaceInviteRepository.save(invite);
    }
}
