package com.teamflow.teamflow.backend.workspaces.api;

import com.jayway.jsonpath.JsonPath;
import com.teamflow.teamflow.backend.auth.AuthTestHelper;
import com.teamflow.teamflow.backend.auth.TestVerificationNotifier;
import com.teamflow.teamflow.backend.auth.security.JwtService;
import com.teamflow.teamflow.backend.support.IntegrationTestBase;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMember;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberId;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMemberRole;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceMemberRepository;
import com.teamflow.teamflow.backend.workspaces.repo.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspacePromoteMemberApiIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestVerificationNotifier notifier;

    @Autowired
    WorkspaceRepository workspaceRepository;

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    JwtService jwtService;

    private AuthTestHelper authTestHelper;
    private String bearer;

    @BeforeEach
    void cleanDb() throws Exception {
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();

        authTestHelper = new AuthTestHelper(mockMvc, notifier);
        bearer = authTestHelper.obtainBearerToken();
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder rb) {
        return rb.header(HttpHeaders.AUTHORIZATION, bearer);
    }

    @Test
    void promoteMember_whenOwnerPromotesMember_shouldReturn204_andRoleBecomesOwner() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        String bearer2 = "Bearer " + accessToken2;
        UUID userId2 = UUID.fromString(jwtService.extractUserId(accessToken2));

        workspaceMemberRepository.save(WorkspaceMember.member(workspaceId, userId2));

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{id}/members/{userId}/promote", workspaceId, userId2))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        WorkspaceMemberId memberId = new WorkspaceMemberId(workspaceId, userId2);
        WorkspaceMember member = workspaceMemberRepository.findById(memberId).orElseThrow();
        assertEquals(WorkspaceMemberRole.OWNER, member.getRole());
    }

    @Test
    void promoteMember_whenActorIsMemberButNotOwner_shouldReturn403_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        String bearer2 = "Bearer " + accessToken2;
        UUID userId2 = UUID.fromString(jwtService.extractUserId(accessToken2));

        workspaceMemberRepository.save(WorkspaceMember.member(workspaceId, userId2));

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/members/{userId}/promote", workspaceId, userId2)
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Only workspace owner can perform this action."));
    }

    @Test
    void promoteMember_whenTargetNotMember_shouldReturn404_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID randomUserId = UUID.randomUUID();

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{id}/members/{userId}/promote", workspaceId, randomUserId))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Member not found."));
    }

    @Test
    void promoteMember_whenTargetAlreadyOwner_shouldReturn409_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        UUID userId2 = UUID.fromString(jwtService.extractUserId(accessToken2));

        workspaceMemberRepository.save(WorkspaceMember.owner(workspaceId, userId2));

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{id}/members/{userId}/promote", workspaceId, userId2))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Member is already an owner."));
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
}