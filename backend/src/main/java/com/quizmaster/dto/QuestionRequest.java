package com.quizmaster.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionRequest(
        @NotBlank(message = "Question text is required") @Size(max = 2000) String text,
        @NotNull @Size(min = 2, max = 8, message = "A question needs 2 to 8 options") List<@Valid OptionRequest> options) {}
