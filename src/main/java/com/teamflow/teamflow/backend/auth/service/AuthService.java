package com.teamflow.teamflow.backend.auth.service;

import com.teamflow.teamflow.backend.auth.api.dto.AuthResponse;
import com.teamflow.teamflow.backend.auth.notify.VerificationNotifier;
import com.teamflow.teamflow.backend.auth.security.EmailVerificationTokenGenerator;
import com.teamflow.teamflow.backend.auth.security.JwtService;
import com.teamflow.teamflow.backend.auth.security.TokenHasher;
import com.teamflow.teamflow.backend.common.errors.BadRequestException;
import com.teamflow.teamflow.backend.common.errors.ConflictException;
import com.teamflow.teamflow.backend.common.errors.NotFoundException;
import com.teamflow.teamflow.backend.users.domain.User;
import com.teamflow.teamflow.backend.users.domain.UserStatus;
import com.teamflow.teamflow.backend.users.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final int VERIFY_TOKEN_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenGenerator tokenGenerator;
    private final TokenHasher tokenHasher;
    private final VerificationNotifier verificationNotifier;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationTokenGenerator tokenGenerator,
            TokenHasher tokenHasher,
            VerificationNotifier verificationNotifier,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.verificationNotifier = verificationNotifier;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegistrationResult register(String username, String email, String rawPassword) {
        String normalizedEmail = email.toLowerCase().strip();
        String normalizedUsername = username.strip().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email is already taken.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ConflictException("Username is already taken.");
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = new User(normalizedUsername, normalizedEmail, passwordHash);

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHasher.sha256Base64Url(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(VERIFY_TOKEN_TTL_HOURS);
        user.startEmailVerification(tokenHash, expiresAt);

        User saved = userRepository.save(user);

        verificationNotifier.sendEmailVerification(saved.getEmail(), rawToken);

        return new RegistrationResult(saved.getId(), saved.getStatus());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String tokenHash = tokenHasher.sha256Base64Url(rawToken);

        User user = userRepository.findByEmailVerificationTokenHash(tokenHash)
                .orElseThrow(() -> new NotFoundException("Verification token is invalid."));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new ConflictException("User is not in PENDING status.");
        }

        LocalDateTime expiresAt = user.getEmailVerificationTokenExpiresAt();
        if (expiresAt == null) {
            throw new BadRequestException("Verification token is invalid.");
        }

        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw new BadRequestException("Verification token has expired.");
        }

        user.verifyEmail(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(String email, String rawPassword) {
        String normalizedEmail = email.strip().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid credentials."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            if (user.getStatus() == UserStatus.PENDING) {
                throw new ConflictException("Email is not verified.");
            }
            throw new ConflictException("User is disabled.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials.");
        }

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(accessToken, "Bearer", jwtService.getTtlSeconds());
    }

    public record RegistrationResult(UUID userId, UserStatus status) {}
}
