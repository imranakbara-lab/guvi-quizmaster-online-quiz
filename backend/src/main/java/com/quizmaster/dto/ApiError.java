package com.quizmaster.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(int status, String error, String message, Map<String, String> fieldErrors, LocalDateTime timestamp) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, null, LocalDateTime.now());
    }
}
