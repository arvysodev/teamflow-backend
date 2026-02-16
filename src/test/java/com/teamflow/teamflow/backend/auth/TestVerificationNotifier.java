package com.teamflow.teamflow.backend.auth;

import com.teamflow.teamflow.backend.auth.notify.VerificationNotifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@Primary
public class TestVerificationNotifier implements VerificationNotifier {

    private final AtomicReference<String> lastToken = new AtomicReference<>();

    @Override
    public void sendEmailVerification(String email, String rawToken) {
        lastToken.set(rawToken);
    }

    public String consumeLastToken() {
        String token = lastToken.getAndSet(null);
        if (token == null) {
            throw new IllegalStateException("No verification token captured. Did you call /register?");
        }
        return token;
    }
}
