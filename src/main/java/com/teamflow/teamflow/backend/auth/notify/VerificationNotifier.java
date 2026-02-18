package com.teamflow.teamflow.backend.auth.notify;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VerificationNotifier {
    void sendEmailVerification(String email, String rawToken);
    void sendWorkspaceInvite(String email, String rawToken, UUID workspaceId, LocalDateTime expiresAt);
}
