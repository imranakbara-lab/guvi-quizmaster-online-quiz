package com.quizmaster.dto;

import com.quizmaster.entity.AnswerOption;

public record OptionAdminView(Long id, String text, boolean correct) {
    public static OptionAdminView from(AnswerOption o) { return new OptionAdminView(o.getId(), o.getText(), o.isCorrect()); }
}
