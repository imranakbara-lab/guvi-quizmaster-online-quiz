package com.quizmaster.dto;

import com.quizmaster.entity.Question;

import java.util.List;

/** Question shown while taking a quiz (no answers). */
public record QuestionView(Long id, String text, boolean multipleCorrect, List<OptionView> options) {
    public static QuestionView from(Question q) {
        return new QuestionView(q.getId(), q.getText(), q.isMultipleCorrect(),
                q.getOptions().stream().map(OptionView::from).toList());
    }
}
