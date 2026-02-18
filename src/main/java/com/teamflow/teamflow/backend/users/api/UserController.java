package com.teamflow.teamflow.backend.users.api;

import com.teamflow.teamflow.backend.users.api.mapper.UserMapper;
import com.teamflow.teamflow.backend.users.domain.User;
import com.teamflow.teamflow.backend.users.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userMapper = userMapper;
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me() {
        User user = userService.getCurrentUser();
        return userMapper.toResponse(user);
    }
}
