package com.teamflow.teamflow.backend.users.api.mapper;

import com.teamflow.teamflow.backend.users.api.dto.UserResponse;
import com.teamflow.teamflow.backend.users.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserResponse toResponse(User user);
}
