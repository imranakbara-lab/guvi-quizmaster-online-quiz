package com.quizmaster.dto;

import java.util.List;

public record AttemptResult(AttemptSummary summary, List<ReviewItem> review) {}
