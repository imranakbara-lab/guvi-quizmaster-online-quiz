package com.quizmaster.dto;

import jakarta.validation.Valid;

import java.util.List;

public record SubmitRequest(List<@Valid AnswerRequest> answers) {}
