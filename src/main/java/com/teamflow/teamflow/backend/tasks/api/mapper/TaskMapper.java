package com.teamflow.teamflow.backend.tasks.api.mapper;

import com.teamflow.teamflow.backend.tasks.api.TaskResponse;
import com.teamflow.teamflow.backend.tasks.domain.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "status", expression = "java(task.getStatus().name())")
    TaskResponse toResponse(Task task);
}
