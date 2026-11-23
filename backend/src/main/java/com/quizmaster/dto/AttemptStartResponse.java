package com.quizmaster.dto;

import java.util.List;

/** Returned when a participant starts (or resumes) a quiz. remainingSeconds drives the timer. */
public record AttemptStartResponse(Long attemptId, Long quizId, String quizTitle, String description,
                                   int timeLimitMinutes, long remainingSeconds, List<QuestionView> questions) {}
