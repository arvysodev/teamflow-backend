package com.teamflow.teamflow.backend.workspaces.api;

import com.jayway.jsonpath.JsonPath;
import com.teamflow.teamflow.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceApiIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Test
    void createWorkspace_shouldReturn201_andResponseBody() throws Exception {
        String body = """
                { "name": "Test" }
                """;

        mockMvc.perform(
                        post("/api/v1/workspaces")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Test"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createWorkSpace_whenBlankName_shouldReturn400_andProblemDetail() throws Exception {
        String body = """
                { "name": "" }
                """;

        mockMvc.perform(
                        post("/api/v1/workspaces")
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
    void createWorkspace_whenNameAlreadyExists_shouldReturn409_andProblemDetail() throws Exception {
        String body = """
                { "name": "Test" }
                """;

        mockMvc.perform(
                        post("/api/v1/workspaces")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Test"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());


        mockMvc.perform(
                        post("/api/v1/workspaces")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Workspace with this name already exists."));
    }

    @Test
    void getWorkspaces_shouldReturn200_andResponseBody() throws Exception {
        createGetTestData();

        mockMvc.perform(
                        get("/api/v1/workspaces")
                                .param("page", "0")
                                .param("size", "100")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(50))
                .andExpect(jsonPath("$.items[0].name").value("Test3"));
    }

    @Test
    void getWorkspaceById_shouldReturn200() throws Exception{
        UUID id = createWorkspaceAndReturnId("Test");

        mockMvc.perform(
                        get("/api/v1/workspaces/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Test"));
    }

    @Test
    void getWorkspaceById_whenWorkspaceNotFound_ShouldReturn404_andProblemDetail() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/v1/workspaces/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Workspace not found."));
    }

    @Test
    void renameWorkspace_shouldReturn200_andResponseBody() throws Exception {
        UUID id = createWorkspaceAndReturnId("Test");
        String body = """
                { "name": " Joe " }
                """;

        mockMvc.perform(
                        patch("/api/v1/workspaces/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Joe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void renameWorkspace_shouldReturn400_andProblemDetail() throws Exception {
        UUID id = createWorkspaceAndReturnId("Test");
        String body = """
                { "name": "" }
                """;

        mockMvc.perform(
                        patch("/api/v1/workspaces/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Missing required value."));
    }

    @Test
    void renameWorkspace_whenNameAlreadyTaken_shouldReturn409_andProblemDetail() throws Exception {
        UUID id = createWorkspaceAndReturnId("Before");
        String body = """
                { "name": " After " }
                """;

        mockMvc.perform(
                        post("/api/v1/workspaces")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        patch("/api/v1/workspaces/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Workspace with this name already exists."));
    }

    @Test
    void renameWorkspace_whenWorkspaceIsClosed_shouldReturn409_andProblemDetial() throws Exception {
        UUID id = createWorkspaceAndReturnId("Before");
        String body = """
                { "name": " After " }
                """;

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/close", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        patch("/api/v1/workspaces/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Closed workspace cannot be renamed."));
    }

    @Test
    void closeWorkspace_shouldReturn204() throws Exception {
        UUID id = createWorkspaceAndReturnId("Test");

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/close", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/workspaces/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeWorkspace_whenAlreadyClosed_shouldReturn409_andProblemDetail() throws Exception {
        UUID id = createWorkspaceAndReturnId("Test");

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/close", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/close", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Workspace is already closed."));
    }

    @Test
    void closeWorkspace_whenWorkspaceNotFound_shouldReturn404_andProblemDetail() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/close", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Workspace not found."));
    }

    @Test
    void restoreWorkspace_shouldReturn204() throws Exception {
        UUID id = createWorkspaceAndReturnId("Test");

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/close", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/restore", id)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/workspaces/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void restoreWorkspace_whenWorkspaceAlreadyActive_shouldReturn409_andProblemDetail() throws Exception {
        UUID id = createWorkspaceAndReturnId("Test");

        mockMvc.perform(
                        get("/api/v1/workspaces/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/restore", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Workspace is already active."));
    }

    @Test
    void restoreWorkspace_whenWorkspaceNotFound_shouldReturn404_andProblemDetail() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/workspaces/{id}/restore", id)
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
                        post("/api/v1/workspaces")
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

    private void createGetTestData() throws Exception{
        String body1 = """
                { "name": "Test1" }
                """;

        mockMvc.perform(
                post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body1)
        )
                .andExpect(status().isCreated());

        String body2 = """
                { "name": "Test2" }
                """;

        mockMvc.perform(
                post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body2)
        )
                .andExpect(status().isCreated());

        String body3 = """
                { "name": "Test3" }
                """;

        mockMvc.perform(
                post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body3)
        )
                .andExpect(status().isCreated());
    }
}
