package com.quizmaster.controller;

import com.quizmaster.dto.UserResponse;
import com.quizmaster.security.UserPrincipal;
import com.quizmaster.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Current logged-in user")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal me) {
        return userService.getById(me.getId());
    }
}
