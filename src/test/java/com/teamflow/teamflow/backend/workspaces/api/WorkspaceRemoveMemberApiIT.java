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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceRemoveMemberApiIT extends IntegrationTestBase {

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
    void removeMember_whenOwnerRemovesMember_shouldReturn204_andDeleteMembership() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        String bearer2 = "Bearer " + accessToken2;
        UUID userId2 = UUID.fromString(jwtService.extractUserId(accessToken2));

        workspaceMemberRepository.save(WorkspaceMember.member(workspaceId, userId2));

        mockMvc.perform(
                        authorized(delete("/api/v1/workspaces/{id}/members/{userId}", workspaceId, userId2))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        assertFalse(workspaceMemberRepository.existsById(new WorkspaceMemberId(workspaceId, userId2)));
    }

    @Test
    void removeMember_whenActorIsMemberButNotOwner_shouldReturn403_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        String bearer2 = "Bearer " + accessToken2;
        UUID userId2 = UUID.fromString(jwtService.extractUserId(accessToken2));

        workspaceMemberRepository.save(WorkspaceMember.member(workspaceId, userId2));

        mockMvc.perform(
                        delete("/api/v1/workspaces/{id}/members/{userId}", workspaceId, userId2)
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
    void removeMember_whenOwnerRemovesSelf_shouldReturn400_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        UUID ownerId = UUID.fromString(JsonPath.read(
                mockMvc.perform(
                                authorized(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/users/me"))
                                        .accept(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        ));

        mockMvc.perform(
                        authorized(delete("/api/v1/workspaces/{id}/members/{userId}", workspaceId, ownerId))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Use leave endpoint to leave the workspace."));
    }

    @Test
    void removeMember_whenTargetOwnerAndMultipleOwners_shouldReturn204_andDeleteMembership() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        UUID ownerId = UUID.fromString(JsonPath.read(
                mockMvc.perform(
                                authorized(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/users/me"))
                                        .accept(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        ));

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        UUID userId2 = UUID.fromString(jwtService.extractUserId(accessToken2));

        workspaceMemberRepository.save(WorkspaceMember.owner(workspaceId, userId2));

        mockMvc.perform(
                        authorized(delete("/api/v1/workspaces/{id}/members/{userId}", workspaceId, userId2))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        assertFalse(workspaceMemberRepository.existsById(new WorkspaceMemberId(workspaceId, userId2)));
        assertTrue(workspaceMemberRepository.existsById(new WorkspaceMemberId(workspaceId, ownerId)));
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