package com.quizmaster.dto;

import com.quizmaster.entity.Quiz;

import java.time.LocalDateTime;
import java.util.List;

/** Full quiz with correct answers - admin only. */
public record QuizDetail(Long id, String title, String description, int timeLimitMinutes, boolean published,
                         LocalDateTime createdAt, List<QuestionAdminView> questions) {
    public static QuizDetail from(Quiz q) {
        return new QuizDetail(q.getId(), q.getTitle(), q.getDescription(), q.getTimeLimitMinutes(), q.isPublished(),
                q.getCreatedAt(), q.getQuestions().stream().map(QuestionAdminView::from).toList());
    }
}
