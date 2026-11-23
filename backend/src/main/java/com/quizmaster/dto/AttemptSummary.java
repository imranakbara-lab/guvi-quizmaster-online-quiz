package com.quizmaster.dto;

import com.quizmaster.entity.QuizAttempt;

import java.time.LocalDateTime;

public record AttemptSummary(Long id, Long quizId, String quizTitle, String userName, String userEmail,
                             int score, int totalQuestions, double percentage, boolean timedOut,
                             LocalDateTime startedAt, LocalDateTime submittedAt) {
    public static AttemptSummary from(QuizAttempt a) {
        return new AttemptSummary(a.getId(), a.getQuiz().getId(), a.getQuiz().getTitle(), a.getUser().getName(),
                a.getUser().getEmail(), a.getScore(), a.getTotalQuestions(), a.getPercentage(), a.isTimedOut(),
                a.getStartedAt(), a.getSubmittedAt());
    }
}
