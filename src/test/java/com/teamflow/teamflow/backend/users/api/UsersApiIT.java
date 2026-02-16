package com.teamflow.teamflow.backend.users.api;

import com.teamflow.teamflow.backend.auth.AuthTestHelper;
import com.teamflow.teamflow.backend.auth.TestVerificationNotifier;
import com.teamflow.teamflow.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UsersApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    TestVerificationNotifier notifier;

    private AuthTestHelper authTestHelper;
    private String bearer;

    @BeforeEach
    void setup() throws Exception {
        authTestHelper = new AuthTestHelper(mockMvc, notifier);
        bearer = authTestHelper.obtainBearerToken();
    }

    @Test
    void me_whenUserNotAuthorized_shouldReturn401_andProblemDetail() throws Exception {
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void me_shouldReturn200_andUserDetails() throws Exception {
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, bearer)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(org.hamcrest.Matchers.containsString("@example.com")))
                .andExpect(jsonPath("$.username").value(org.hamcrest.Matchers.startsWith("testuser_")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }
}
