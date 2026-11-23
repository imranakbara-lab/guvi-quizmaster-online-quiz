package com.quizmaster.dto;

import com.quizmaster.entity.Quiz;

import java.time.LocalDateTime;

public record QuizSummary(Long id, String title, String description, int timeLimitMinutes,
                          int questionCount, boolean published, LocalDateTime createdAt) {
    public static QuizSummary from(Quiz q) {
        return new QuizSummary(q.getId(), q.getTitle(), q.getDescription(), q.getTimeLimitMinutes(),
                q.getQuestions().size(), q.isPublished(), q.getCreatedAt());
    }
}
