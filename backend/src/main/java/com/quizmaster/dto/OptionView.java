package com.quizmaster.dto;

import com.quizmaster.entity.AnswerOption;

/** Option shown to a participant - the correct flag is hidden. */
public record OptionView(Long id, String text) {
    public static OptionView from(AnswerOption o) { return new OptionView(o.getId(), o.getText()); }
}
