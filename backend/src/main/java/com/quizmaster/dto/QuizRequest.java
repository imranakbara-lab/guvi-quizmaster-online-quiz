package com.quizmaster.dto;

import jakarta.validation.constraints.*;

public record QuizRequest(
        @NotBlank(message = "Title is required") @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotNull(message = "Time limit is required") @Min(value = 1, message = "Time limit must be at least 1 minute")
        @Max(value = 180, message = "Time limit cannot exceed 180 minutes") Integer timeLimitMinutes,
        boolean published) {}
