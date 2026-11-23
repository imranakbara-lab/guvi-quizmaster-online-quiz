package com.quizmaster.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AnswerRequest(@NotNull Long questionId, List<Long> selectedOptionIds) {}
