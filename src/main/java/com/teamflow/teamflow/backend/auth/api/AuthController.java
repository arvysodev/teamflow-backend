package com.teamflow.teamflow.backend.auth.api;

import com.teamflow.teamflow.backend.auth.api.dto.*;
import com.teamflow.teamflow.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
        var result = authService.register(req.username(), req.email(), req.password());

        return new RegisterResponse(
                result.userId(),
                result.status().name(),
                "Verification email sent."
        );
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        authService.verifyEmail(req.token());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.email(), req.password());
    }
}
