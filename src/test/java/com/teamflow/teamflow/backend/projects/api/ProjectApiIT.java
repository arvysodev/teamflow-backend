package com.teamflow.teamflow.backend.projects.api;

import com.jayway.jsonpath.JsonPath;
import com.teamflow.teamflow.backend.auth.AuthTestHelper;
import com.teamflow.teamflow.backend.auth.TestVerificationNotifier;
import com.teamflow.teamflow.backend.auth.security.JwtService;
import com.teamflow.teamflow.backend.projects.repo.ProjectRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectApiIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestVerificationNotifier notifier;

    @Autowired
    WorkspaceRepository workspaceRepository;

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    JwtService jwtService;

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
    void createProject_shouldReturn201_andResponseBody() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        String body = """
                { "name": "Project A" }
                """;

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects", workspaceId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$.name").value("Project A"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdBy").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createProject_whenNameBlank_shouldReturn400_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        String body = """
                { "name": "" }
                """;

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects", workspaceId))
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
    void listProjects_shouldReturn200_andPageResponse() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());

        UUID p1 = createProjectAndReturnId(workspaceId, "P1");
        UUID p2 = createProjectAndReturnId(workspaceId, "P2");

        mockMvc.perform(
                        authorized(get("/api/v1/workspaces/{workspaceId}/projects", workspaceId))
                                .param("page", "0")
                                .param("size", "50")
                                .param("status", "ACTIVE")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").isNotEmpty())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(50))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(1));

        assertNotNull(p1);
        assertNotNull(p2);
    }

    @Test
    void getProjectById_shouldReturn200_andResponseBody() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project X");

        mockMvc.perform(
                        authorized(get("/api/v1/workspaces/{workspaceId}/projects/{id}", workspaceId, projectId))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$.name").value("Project X"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void renameProject_whenOwner_shouldReturn200_andUpdatedName() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Before");

        String body = """
                { "name": " After " }
                """;

        mockMvc.perform(
                        authorized(patch("/api/v1/workspaces/{workspaceId}/projects/{id}", workspaceId, projectId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void renameProject_whenActorNotOwner_shouldReturn403_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Before");

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        String bearer2 = "Bearer " + accessToken2;
        UUID userId2 = UUID.fromString(jwtService.extractUserId(accessToken2));

        workspaceMemberRepository.save(WorkspaceMember.member(workspaceId, userId2));

        String body = """
                { "name": "After" }
                """;

        mockMvc.perform(
                        patch("/api/v1/workspaces/{workspaceId}/projects/{id}", workspaceId, projectId)
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Only workspace owner can perform this action."));
    }

    @Test
    void archiveProject_shouldReturn204_andThenListArchivedContainsIt() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "ToArchive");

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{id}/archive", workspaceId, projectId))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        authorized(get("/api/v1/workspaces/{workspaceId}/projects", workspaceId))
                                .param("page", "0")
                                .param("size", "50")
                                .param("status", "ARCHIVED")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(projectId.toString()))
                .andExpect(jsonPath("$.items[0].status").value("ARCHIVED"));
    }

    @Test
    void restoreProject_shouldReturn204_andThenListActiveContainsIt() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "ToRestore");

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{id}/archive", workspaceId, projectId))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{id}/restore", workspaceId, projectId))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        authorized(get("/api/v1/workspaces/{workspaceId}/projects", workspaceId))
                                .param("page", "0")
                                .param("size", "50")
                                .param("status", "ACTIVE")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(projectId.toString()))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"));
    }

    @Test
    void getProject_whenUserNotWorkspaceMember_shouldReturn404_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Secret");

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String bearer2 = auth2.obtainBearerToken();

        mockMvc.perform(
                        get("/api/v1/workspaces/{workspaceId}/projects/{id}", workspaceId, projectId)
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Workspace not found."));
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

    private UUID createProjectAndReturnId(UUID workspaceId, String name) throws Exception {
        String body = """
                { "name": "%s" }
                """.formatted(name);

        MvcResult result = mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects", workspaceId))
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