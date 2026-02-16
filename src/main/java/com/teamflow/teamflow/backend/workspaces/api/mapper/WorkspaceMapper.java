package com.teamflow.teamflow.backend.workspaces.api.mapper;

import com.teamflow.teamflow.backend.workspaces.api.WorkspaceResponse;
import com.teamflow.teamflow.backend.workspaces.domain.Workspace;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {
    WorkspaceResponse toResponse(Workspace workspace);
}
