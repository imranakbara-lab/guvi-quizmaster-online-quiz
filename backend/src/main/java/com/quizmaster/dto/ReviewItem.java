package com.quizmaster.dto;

import java.util.List;

/** One question in the result review: what was picked vs what was correct. */
public record ReviewItem(Long questionId, String text, List<OptionAdminView> options,
                         List<Long> selectedOptionIds, boolean correct) {}
