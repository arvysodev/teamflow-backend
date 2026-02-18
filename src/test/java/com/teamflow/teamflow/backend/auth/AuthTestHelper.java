package com.teamflow.teamflow.backend.auth;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthTestHelper {

    private final MockMvc mockMvc;
    private final TestVerificationNotifier notifier;

    public AuthTestHelper(MockMvc mockMvc, TestVerificationNotifier notifier) {
        this.mockMvc = mockMvc;
        this.notifier = notifier;
    }

    public String obtainAccessToken() throws Exception {
        String username = "testuser_" + UUID.randomUUID();
        String email = "user_" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";

        String registerBody = """
                { "username": "%s", "email": "%s", "password": "%s" }
                """.formatted(username, email, password);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(registerBody)
                )
                .andExpect(status().isCreated());

        String verificationToken = notifier.consumeLastEmailVerificationToken();

        String verifyBody = """
                { "token": "%s" }
                """.formatted(verificationToken);

        mockMvc.perform(
                        post("/api/v1/auth/verify-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(verifyBody)
                )
                .andExpect(status().isNoContent());

        String loginBody = """
                { "email": "%s", "password": "%s" }
                """.formatted(email, password);

        var loginResult = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isOk())
                .andReturn();

        String json = loginResult.getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }

    public String obtainBearerToken() throws Exception {
        return "Bearer " + obtainAccessToken();
    }

    public String authHeaderName() {
        return HttpHeaders.AUTHORIZATION;
    }
}
