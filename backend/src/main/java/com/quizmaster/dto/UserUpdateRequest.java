package com.quizmaster.dto;

import com.quizmaster.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Used by Admin to edit a user. */
public record UserUpdateRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email String email,
        String phone,
        @NotNull(message = "Role is required") Role role) {}
