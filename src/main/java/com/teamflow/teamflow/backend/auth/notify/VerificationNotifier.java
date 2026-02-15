package com.teamflow.teamflow.backend.auth.notify;

public interface VerificationNotifier {
    void sendEmailVerification(String email, String rawToken);
}
