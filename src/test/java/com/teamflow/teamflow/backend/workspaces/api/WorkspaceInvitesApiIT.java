package com.teamflow.teamflow.backend.workspaces.api;

import com.jayway.jsonpath.JsonPath;
import com.teamflow.teamflow.backend.auth.AuthTestHelper;
import com.teamflow.teamflow.backend.auth.TestVerificationNotifier;
import com.teamflow.teamflow.backend.support.IntegrationTestBase;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceInviteRepository;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceMemberRepository;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceInviteApiIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestVerificationNotifier notifier;

    @Autowired
    WorkspaceRepository workspaceRepository;

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    WorkspaceInviteRepository workspaceInviteRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private AuthTestHelper authTestHelper;
    private String bearer;

    @BeforeEach
    void cleanDb() throws Exception {
        workspaceMemberRepository.deleteAll();
        workspaceInviteRepository.deleteAll();
        workspaceRepository.deleteAll();

        authTestHelper = new AuthTestHelper(mockMvc, notifier);
        bearer = authTestHelper.obtainBearerToken();
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder rb) {
        return rb.header(HttpHeaders.AUTHORIZATION, bearer);
    }

    @Test
    void invite_shouldReturn201_andResponseBody() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        String email = "member_" + UUID.randomUUID() + "@example.com";

        String body = """
                { "email": "%s" }
                """.formatted(email);

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/invites/{id}", workspaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("Invitation sent.")));

        String token = notifier.consumeLastWorkspaceInviteToken();
        org.junit.jupiter.api.Assertions.assertNotNull(token);
        org.junit.jupiter.api.Assertions.assertFalse(token.isBlank());
    }

    @Test
    void invite_whenActiveInviteAlreadyExists_shouldReturn409_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        String email = "member_" + UUID.randomUUID() + "@example.com";

        invite(workspaceId, email);

        String body = """
                { "email": "%s" }
                """.formatted(email);

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/invites/{id}", workspaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Active invite already exists for this email."));
    }

    @Test
    void invite_whenEmailBlank_shouldReturn400_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        String body = """
                { "email": "  " }
                """;

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/invites/{id}", workspaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Missing required value."));
    }

    @Test
    void accept_shouldReturn204_andCreateMembership() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String email2 = "member_" + UUID.randomUUID() + "@example.com";
        String bearer2 = auth2.obtainBearerToken(email2);

        invite(workspaceId, email2);
        String token = notifier.consumeLastWorkspaceInviteToken();

        String acceptBody = """
                { "rawToken": "%s" }
                """.formatted(token);

        mockMvc.perform(
                        post("/api/v1/workspaces/invites/accept")
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(acceptBody)
                )
                .andExpect(status().isNoContent());

        UUID userId2 = getUserIdFromMe(bearer2);

        org.junit.jupiter.api.Assertions.assertTrue(
                workspaceMemberRepository.existsByIdWorkspaceIdAndIdUserId(workspaceId, userId2)
        );
    }

    @Test
    void accept_whenTokenInvalid_shouldReturn404_andProblemDetail() throws Exception {
        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String bearer2 = auth2.obtainBearerToken();

        String body = """
                { "rawToken": "not-a-real-token" }
                """;

        mockMvc.perform(
                        post("/api/v1/workspaces/invites/accept")
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Invite token is invalid."));
    }

    @Test
    void accept_whenInviteExpired_shouldReturn400_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String email2 = "member_" + UUID.randomUUID() + "@example.com";
        String bearer2 = auth2.obtainBearerToken(email2);

        invite(workspaceId, email2);
        String token = notifier.consumeLastWorkspaceInviteToken();

        expireInvitesByEmail(workspaceId, email2);

        String body = """
                { "rawToken": "%s" }
                """.formatted(token);

        mockMvc.perform(
                        post("/api/v1/workspaces/invites/accept")
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Invite token has expired."));
    }

    @Test
    void accept_whenInviteAlreadyAccepted_shouldReturn409_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String email2 = "member_" + UUID.randomUUID() + "@example.com";
        String bearer2 = auth2.obtainBearerToken(email2);

        invite(workspaceId, email2);
        String token = notifier.consumeLastWorkspaceInviteToken();

        acceptAs(bearer2, token);

        String body = """
                { "rawToken": "%s" }
                """.formatted(token);

        mockMvc.perform(
                        post("/api/v1/workspaces/invites/accept")
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Invite is already accepted."));
    }

    @Test
    void accept_whenEmailDoesNotMatch_shouldReturn403_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String email2 = "member_" + UUID.randomUUID() + "@example.com";
        String bearer2 = auth2.obtainBearerToken(email2);

        invite(workspaceId, email2);
        String token = notifier.consumeLastWorkspaceInviteToken();

        String body = """
                { "rawToken": "%s" }
                """.formatted(token);

        mockMvc.perform(
                        post("/api/v1/workspaces/invites/accept")
                                .header(HttpHeaders.AUTHORIZATION, bearer)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("This invite was issued for a different email."));
    }

    @Test
    void accept_whenUserAlreadyMember_shouldReturn409_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String email2 = "member_" + UUID.randomUUID() + "@example.com";
        String bearer2 = auth2.obtainBearerToken(email2);

        invite(workspaceId, email2);
        String token1 = notifier.consumeLastWorkspaceInviteToken();
        acceptAs(bearer2, token1);

        expireInvitesByEmail(workspaceId, email2);

        invite(workspaceId, email2);
        String token2 = notifier.consumeLastWorkspaceInviteToken();

        String body = """
                { "rawToken": "%s" }
                """.formatted(token2);

        mockMvc.perform(
                        post("/api/v1/workspaces/invites/accept")
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("User is already a member."));
    }

    @Test
    void accept_whenTokenBlank_shouldReturn400_andProblemDetail() throws Exception {
        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String bearer2 = auth2.obtainBearerToken();

        String body = """
                { "rawToken": " " }
                """;

        mockMvc.perform(
                        post("/api/v1/workspaces/invites/accept")
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Missing required value."));
    }

    private UUID createWorkspaceAndReturnId(String name) throws Exception {
        String body = """
                { "name": "%s" }
                """.formatted(name);

        MvcResult result = mockMvc.perform(
                        authorized(post("/api/v1/workspaces"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                JsonPath.read(result.getResponse().getContentAsString(), "$.id")
        );
    }

    private void invite(UUID workspaceId, String email) throws Exception {
        String body = """
                { "email": "%s" }
                """.formatted(email);

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/invites/{id}", workspaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated());
    }

    private void acceptAs(String bearerValue, String rawToken) throws Exception {
        String body = """
                { "rawToken": "%s" }
                """.formatted(rawToken);

        mockMvc.perform(
                        post("/api/v1/workspaces/invites/accept")
                                .header(HttpHeaders.AUTHORIZATION, bearerValue)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isNoContent());
    }

    private UUID getUserIdFromMe(String bearerValue) throws Exception {
        var meResult = mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, bearerValue)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        String meJson = meResult.getResponse().getContentAsString();
        String userId = JsonPath.read(meJson, "$.id");
        return UUID.fromString(userId);
    }

    private void expireInvitesByEmail(UUID workspaceId, String email) {
        jdbcTemplate.update(
                "UPDATE workspace_invites SET expires_at = ? WHERE workspace_id = ? AND email = ?",
                LocalDateTime.now().minusMinutes(1),
                workspaceId,
                email.toLowerCase().strip()
        );
    }
}