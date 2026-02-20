package com.teamflow.teamflow.backend.workspaces.api;

import com.jayway.jsonpath.JsonPath;
import com.teamflow.teamflow.backend.auth.AuthTestHelper;
import com.teamflow.teamflow.backend.auth.TestVerificationNotifier;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceLeaveApiIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestVerificationNotifier notifier;

    @Autowired
    WorkspaceRepository workspaceRepository;

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;

    private AuthTestHelper authTestHelper;
    private String bearer;

    @BeforeEach
    void cleanDb() throws Exception {
        cleanDatabase();

        authTestHelper = new AuthTestHelper(mockMvc, notifier);
        bearer = authTestHelper.obtainBearerToken();
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder rb) {
        return rb.header(HttpHeaders.AUTHORIZATION, bearer);
    }

    @Test
    void leave_whenMember_shouldReturn204_andDeleteMembership() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        String bearer2 = "Bearer " + accessToken2;

        UUID userId2 = UUID.fromString(JsonPath.read(
                mockMvc.perform(get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        ));

        workspaceMemberRepository.save(WorkspaceMember.member(workspaceId, userId2));

        mockMvc.perform(
                        delete("/api/v1/workspaces/{id}/leave", workspaceId)
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        assertFalse(workspaceMemberRepository.existsById(new WorkspaceMemberId(workspaceId, userId2)));
    }

    @Test
    void leave_whenOnlyOwner_shouldReturn409_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        mockMvc.perform(
                        delete("/api/v1/workspaces/{id}/leave", workspaceId)
                                .header(HttpHeaders.AUTHORIZATION, bearer)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Cannot leave workspace as the only owner."));
    }

    @Test
    void leave_whenOwnerAndAnotherOwnerExists_shouldReturn204_andDeleteMembership() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        UUID ownerId = UUID.fromString(JsonPath.read(
                mockMvc.perform(authorized(get("/api/v1/users/me")).accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        ));

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        String bearer2 = "Bearer " + accessToken2;

        UUID userId2 = UUID.fromString(JsonPath.read(
                mockMvc.perform(get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        ));

        workspaceMemberRepository.save(WorkspaceMember.owner(workspaceId, userId2));

        mockMvc.perform(
                        delete("/api/v1/workspaces/{id}/leave", workspaceId)
                                .header(HttpHeaders.AUTHORIZATION, bearer)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        assertFalse(workspaceMemberRepository.existsById(new WorkspaceMemberId(workspaceId, ownerId)));
        assertTrue(workspaceMemberRepository.existsById(new WorkspaceMemberId(workspaceId, userId2)));
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
