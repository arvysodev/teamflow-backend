package com.teamflow.teamflow.backend.auth.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class LogVerificationNotifier implements VerificationNotifier {

    private static final Logger log = LoggerFactory.getLogger(LogVerificationNotifier.class);

    @Override
    public void sendEmailVerification(String email, String rawToken) {
        log.info("Email verification for {}: token={}", email, rawToken);
    }

    @Override
    public void sendWorkspaceInvite(String email, String rawToken, UUID workspaceId, LocalDateTime expiresAt) {
        log.info("Workspace invite: email={}, workspaceId={}, token={}, expiresAt={}",
                email, workspaceId, rawToken, expiresAt);
    }
}
