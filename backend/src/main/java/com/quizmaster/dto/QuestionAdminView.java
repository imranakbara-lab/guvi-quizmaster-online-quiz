package com.quizmaster.dto;

import com.quizmaster.entity.Question;

import java.util.List;

public record QuestionAdminView(Long id, String text, int position, List<OptionAdminView> options) {
    public static QuestionAdminView from(Question q) {
        return new QuestionAdminView(q.getId(), q.getText(), q.getPosition(),
                q.getOptions().stream().map(OptionAdminView::from).toList());
    }
}
