package com.teamflow.teamflow.backend.auth;

import com.teamflow.teamflow.backend.auth.notify.VerificationNotifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Primary
public class TestVerificationNotifier implements VerificationNotifier {

    private final AtomicReference<String> lastEmailVerificationToken = new AtomicReference<>();
    private final AtomicReference<String> lastWorkspaceInviteToken = new AtomicReference<>();

    @Override
    public void sendEmailVerification(String email, String rawToken) {
        lastEmailVerificationToken.set(rawToken);
    }

    @Override
    public void sendWorkspaceInvite(String email, String rawToken, UUID workspaceId, LocalDateTime expiresAt) {
        lastWorkspaceInviteToken.set(rawToken);
    }

    public String consumeLastEmailVerificationToken() {
        String token = lastEmailVerificationToken.getAndSet(null);
        if (token == null) {
            throw new IllegalStateException("No email verification token captured. Did you call /register?");
        }
        return token;
    }

    public String consumeLastWorkspaceInviteToken() {
        String token = lastWorkspaceInviteToken.getAndSet(null);
        if (token == null) {
            throw new IllegalStateException("No workspace invite token captured. Did you call /workspaces/{id}/invites?");
        }
        return token;
    }
}
