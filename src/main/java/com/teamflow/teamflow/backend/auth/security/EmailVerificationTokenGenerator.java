package com.teamflow.teamflow.backend.auth.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EmailVerificationTokenGenerator {

    private static final int BYTES = 32;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
