package com.teamflow.teamflow.backend.workspaces.api;


import com.teamflow.teamflow.backend.workspaces.service.WorkspaceInviteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/invites")
public class WorkspaceInviteController {

    private final WorkspaceInviteService workspaceInviteService;

    public WorkspaceInviteController(WorkspaceInviteService workspaceInviteService) {
        this.workspaceInviteService = workspaceInviteService;
    }

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateWorkspaceInviteResponse invite(
            @PathVariable UUID id,
            @Valid @RequestBody CreateWorkspaceInviteRequest req
    ) {
        workspaceInviteService.invite(id, req.email());
        return new CreateWorkspaceInviteResponse("Invitation sent.");
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@Valid @RequestBody AcceptWorkspaceInviteRequest req) {
        workspaceInviteService.acceptInvite(req.rawToken());
    }
}
