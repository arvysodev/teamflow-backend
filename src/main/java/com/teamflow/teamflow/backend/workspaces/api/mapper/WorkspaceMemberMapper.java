package com.teamflow.teamflow.backend.workspaces.api.mapper;

import com.teamflow.teamflow.backend.workspaces.api.WorkspaceMemberResponse;
import com.teamflow.teamflow.backend.workspaces.domain.WorkspaceMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkspaceMemberMapper {

    @Mapping(target = "userId", source = "id.userId")
    @Mapping(target = "role", expression = "java(member.getRole().name())")
    WorkspaceMemberResponse toResponse(WorkspaceMember member);
}
