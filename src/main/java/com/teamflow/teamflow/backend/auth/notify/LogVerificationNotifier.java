package com.teamflow.teamflow.backend.auth.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogVerificationNotifier implements VerificationNotifier {

    private static final Logger log = LoggerFactory.getLogger(LogVerificationNotifier.class);

    @Override
    public void sendEmailVerification(String email, String rawToken) {
        log.info("Email verification for {}: token={}", email, rawToken);
    }
}
