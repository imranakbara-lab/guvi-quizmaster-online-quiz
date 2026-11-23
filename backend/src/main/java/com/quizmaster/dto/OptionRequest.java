package com.quizmaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OptionRequest(@NotBlank(message = "Option text is required") @Size(max = 1000) String text, boolean correct) {}
