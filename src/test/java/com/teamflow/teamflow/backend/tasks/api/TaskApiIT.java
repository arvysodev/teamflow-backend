package com.teamflow.teamflow.backend.tasks.api;

import com.jayway.jsonpath.JsonPath;
import com.teamflow.teamflow.backend.auth.AuthTestHelper;
import com.teamflow.teamflow.backend.auth.TestVerificationNotifier;
import com.teamflow.teamflow.backend.auth.security.JwtService;
import com.teamflow.teamflow.backend.projects.repo.ProjectRepository;
import com.teamflow.teamflow.backend.support.IntegrationTestBase;
import com.teamflow.teamflow.backend.tasks.repo.TaskRepository;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMember;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIT extends IntegrationTestBase {

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
    TaskRepository taskRepository;

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
    void createTask_shouldReturn201_andResponseBody() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());

        String body = """
                { "title": "Task A", "description": "Desc" }
                """;

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks", workspaceId, projectId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.title").value("Task A"))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assigneeUserId").doesNotExist())
                .andExpect(jsonPath("$.createdBy").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createTask_whenTitleBlank_shouldReturn400_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());

        String body = """
                { "title": "" }
                """;

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks", workspaceId, projectId))
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
    void listTasks_shouldReturn200_andPageResponse() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());

        UUID t1 = createTaskAndReturnId(workspaceId, projectId, "T1");
        UUID t2 = createTaskAndReturnId(workspaceId, projectId, "T2");

        mockMvc.perform(
                        authorized(get("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks", workspaceId, projectId))
                                .param("page", "0")
                                .param("size", "50")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(50))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(1));

        org.junit.jupiter.api.Assertions.assertNotNull(t1);
        org.junit.jupiter.api.Assertions.assertNotNull(t2);
    }

    @Test
    void getTaskById_shouldReturn200_andResponseBody() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());
        UUID taskId = createTaskAndReturnId(workspaceId, projectId, "Hello");

        mockMvc.perform(
                        authorized(get("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}", workspaceId, projectId, taskId))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.title").value("Hello"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void updateTask_shouldReturn200_andUpdatedFields() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());
        UUID taskId = createTaskAndReturnId(workspaceId, projectId, "Before");

        String body = """
                { "title": " After ", "description": "  Desc  " }
                """;

        mockMvc.perform(
                        authorized(patch("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}", workspaceId, projectId, taskId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.title").value("After"))
                .andExpect(jsonPath("$.description").value("Desc"));
    }

    @Test
    void changeStatus_shouldReturn200_andUpdatedStatus() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());
        UUID taskId = createTaskAndReturnId(workspaceId, projectId, "T");

        String body = """
                { "status": "IN_PROGRESS" }
                """;

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}/status", workspaceId, projectId, taskId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatus_whenInvalidStatus_shouldReturn400_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());
        UUID taskId = createTaskAndReturnId(workspaceId, projectId, "T");

        String body = """
                { "status": "NOPE" }
                """;

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}/status", workspaceId, projectId, taskId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Invalid task status."));
    }

    @Test
    void assignTask_shouldReturn200_andAssigneeSet() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());
        UUID taskId = createTaskAndReturnId(workspaceId, projectId, "T");

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        String bearer2 = "Bearer " + accessToken2;
        UUID userId2 = UUID.fromString(jwtService.extractUserId(accessToken2));

        workspaceMemberRepository.save(WorkspaceMember.member(workspaceId, userId2));

        String body = """
                { "userId": "%s" }
                """.formatted(userId2);

        mockMvc.perform(
                        post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}/assign", workspaceId, projectId, taskId)
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.assigneeUserId").value(userId2.toString()));
    }

    @Test
    void assignTask_whenAssigneeNotWorkspaceMember_shouldReturn400_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());
        UUID taskId = createTaskAndReturnId(workspaceId, projectId, "T");

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String accessToken2 = auth2.obtainAccessToken();
        String bearer2 = "Bearer " + accessToken2;

        UUID actorId2 = UUID.fromString(jwtService.extractUserId(accessToken2));
        workspaceMemberRepository.save(WorkspaceMember.member(workspaceId, actorId2));

        UUID randomUserId = UUID.randomUUID();

        String body = """
            { "userId": "%s" }
            """.formatted(randomUserId);

        mockMvc.perform(
                        post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}/assign", workspaceId, projectId, taskId)
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Assignee must be a workspace member."));
    }

    @Test
    void unassignTask_shouldReturn200_andAssigneeCleared() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());
        UUID taskId = createTaskAndReturnId(workspaceId, projectId, "T");

        UUID meId = UUID.fromString(JsonPath.read(
                mockMvc.perform(
                                authorized(get("/api/v1/users/me"))
                                        .accept(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id"
        ));

        String assignBody = """
                { "userId": "%s" }
                """.formatted(meId);

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}/assign", workspaceId, projectId, taskId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(assignBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeUserId").value(meId.toString()));

        mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks/{id}/unassign", workspaceId, projectId, taskId))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeUserId").doesNotExist());
    }

    @Test
    void listTasks_whenUserNotWorkspaceMember_shouldReturn404_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID projectId = createProjectAndReturnId(workspaceId, "Project_" + UUID.randomUUID());
        createTaskAndReturnId(workspaceId, projectId, "T");

        AuthTestHelper auth2 = new AuthTestHelper(mockMvc, notifier);
        String bearer2 = auth2.obtainBearerToken();

        mockMvc.perform(
                        get("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks", workspaceId, projectId)
                                .header(HttpHeaders.AUTHORIZATION, bearer2)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Workspace not found."));
    }

    @Test
    void listTasks_whenProjectNotFound_shouldReturn404_andProblemDetail() throws Exception {
        UUID workspaceId = createWorkspaceAndReturnId("Ws_" + UUID.randomUUID());
        UUID randomProjectId = UUID.randomUUID();

        mockMvc.perform(
                        authorized(get("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks", workspaceId, randomProjectId))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Project not found."));
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

    private UUID createTaskAndReturnId(UUID workspaceId, UUID projectId, String title) throws Exception {
        String body = """
                { "title": "%s" }
                """.formatted(title);

        MvcResult result = mockMvc.perform(
                        authorized(post("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks", workspaceId, projectId))
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
