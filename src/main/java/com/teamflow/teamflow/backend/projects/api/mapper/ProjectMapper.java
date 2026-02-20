package com.teamflow.teamflow.backend.projects.api.mapper;

import com.teamflow.teamflow.backend.projects.api.ProjectResponse;
import com.teamflow.teamflow.backend.projects.domain.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "status", expression = "java(project.getStatus().name())")
    ProjectResponse toResponse(Project project);
}
